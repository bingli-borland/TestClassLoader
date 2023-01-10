# TestClassLoader

#### 介绍
1. 案例中存在A.class和B.class类,分别在a.jar和b.jar中
A.class
```java
public class A {
  private B b = new B();
  
  public void clear() {
    this.b = null;
  }
}
```
B.class
```java
public class B {}
```
2. MyTest是测试主类，TestLoader为自定义类加载器加载A.class，MyClassLoader中包含多个ModuleLoader(加载B.class)，类加载层次结构如下
```
                                     AppClassLoader
                                          /    \
MyClassLoader(引用ModuleLoader(b.jar)去加载) ---- ModuleLoader(b.jar)         
      |      
TestLoader(a.jar) 
```
3. TestLoader在加载A.class创建对象会触发B.class加载，loadClass方法中判断加载B.class会委托给parent(MyClassLoader)去加载
#### 测试流程
1. 构造ModuleLoader(b.jar),变量名为b1Loader,并添加到MyClassLoader中
2. 构造TestLoader(a.jar)并加载创建A.class对象
3. 从MyClassLoader中移除b1Loader
4. 根据参数确定是否执行System.gc(), 用来清除ClassLoader
5. 再次构造ModuleLoader(b.jar),变量名为b2Loader,并添加到MyClassLoader中
6. 通过Class.forName("B", false, MyClassLoader) 和 MyClassLoader.loadClass("B")加载B类

#### 现象
1. 如上面步骤4执行gc，则jdk8u-192和jdk8u-201在步骤6的现象不同，在jdk8u-192当中：
```java
Class.forName("B", false, MyClassLoader)返回b2Loader加载的B.class
MyClassLoader.loadClass("B")返回b2Loader加载的B.class
两者相同
```
在jdk8u-201中：
```java
Class.forName("B", false, MyClassLoader)返回b1Loader加载的B.class
MyClassLoader.loadClass("B")返回b2Loader加载的B.class
两者不相同
```
2. 如上面步骤4不执行gc，则jdk8u-192和jdk8u-201在步骤6的现象一样:
```java
Class.forName("B", false, MyClassLoader)返回b1Loader加载的B.class
MyClassLoader.loadClass("B")返回b2Loader加载的B.class
两者不相同
```
#### 问题与分析
jvm有几个关键类ClassLoaderData.cpp, SystemDictionary.cpp
##### ClassLoaderData
每个类加载器都会对应一个 ClassLoaderData 的数据结构，里面会存譬如具体的类加载器对象，加载的 klass,这里关注的属性：
- _keep_alive : 如果这个值是 true，那这个类加载器会认为是活的，会将其做为 GC ROOT 的一部分，gc 的时候不会被回收
- _unloading : true表示这个类加载器已经卸载了
##### SystemDictionary
SystemDictionary类的定义在classfile/systemDictionary.hpp中，是一个系统字典类，用于保存所有已经加载完成的类，
通过一个支持自动扩容的HashMap保存，key是表示类名Symbol指针和对应的类加载器oop指针，value是对应的Klass指针，
当一个新的类加载完成后就会在SystemDictionary中添加一个新的键值对，关键属性：
- _dictionary：Dictionary类指针，实际保存已加载类的HashMap,DictionaryEntry为其中一个条目，其创建需要loader_data和klass


1. 对上面现象1,分析了OpenJDK的native代码，发现directory.cpp的实现，在https://github.com/adoptium/jdk8u//commit/4b9ec1c4fa735e97caea5bd73555ce131be60ca5
   提交去掉了一部分内容，基本确认是他影响的。OpenJDK为什么进行这一操作，并没有明确说明。

Class.forName("B", false, MyClassLoader)加载过程中，SystemDictionary.resolve_instance_class_or_null方法会先
从dictionary字典中查找要加载的类, 此处直接找到后返回Klass, 不会再委托给MyClassLoader加载，但是按道理执行gc之后
ModuleLoader已经被清除了，没有任何强引用。对与Klass来说，有initialloader和defineloader两个概念，这个案例中B.class的
initialloader是MyClassLoader，defineloader就是ModuleLoader，gc时会将ModuleLoader对应ClassLoaderData中unloading设置为true，
最后清理SystemDictionary字典中对应DictionaryEntry。清理逻辑大致如下：
- 遍历一条DictionaryEntry，获取对应klass，获取klass对应loader_data(initialloader)的ClassLoaderData
- 判断initialloader是否为强引用，否则继续判断loader_data->is_unloading，是则清除entry
- 否则获取klass的define_loader_data，判断define_loader_data->is_unloading，是则清除entry

此案例中B.class对应loader_data为MyClassLoader, define_loader_data为ModuleLoader，gc时ModuleLoader会设置unloading=true
但是MyClassLoader不会，结合上面提交记录，将define_loader_data的判断逻辑删除了，因此DictionaryEntry并没有删除。


2. Class.forName这个接口。在执行System.gc前后，输出的结果不相同。
从原理上分析由于ClassLoader都没有卸载，因此entry肯定也不会清除，所以加载的结果不同。
从理论上fullGC不应该影响接口的输出结果，因为GC是不定期的。

#### 案例运行
 在jdk8u-192和jdk8u-201(高于192版本即可)运行**mvn clean test**,  testLoadAfterGC在jdk8u-201会失败，和现象1一样