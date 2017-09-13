[TOC]

# Tomcat源码分析

## 参考博客及书籍

[看透springMvc源代码分析与实践.pdf](链接：http://pan.baidu.com/s/1o7Zp1Q6 密码：c87j)

[Tomcat源码中ObjectName这个类的作用](http://blog.csdn.net/wgw335363240/article/details/6123665)

## 1. conf/配置文件说明

### 1.1 catalina.properties

Tomcat的catalina.properties文件位于%CATALINA_HOME%/conf/目录下面，该文件主要配置tomcat的安全设置、类加载设置、不需要扫描的类设置、字符缓存设置四大块。

- 安全设置

  ```properties
  package.access=sun.,org.apache.catalina.,org.apache.coyote.,org.apache.jasper.,org.apache.tomcat.
  package.definition=sun.,java.,org.apache.catalina.,org.apache.coyote.,org.apache.jasper.,org.apache.naming.,org.apache.tomcat.
  ```

- 类加载设置

  >  tomcat的类加载顺序为：

  Bootstrap ---> System ---> /WEB-INF/classes ---> /WEB-INF/lib/*.jar ---> Common

  注: Common的配置是通过catalina.properties的commons.loader设置的

  ```properties
  common.loader="${catalina.base}/lib","${catalina.base}/lib/*.jar","${catalina.home}/lib","${catalina.home}/lib/*.jar"
  ```

  >  类加载顺序：

  ${catalina.base}/lib  未打包的类和资源文件

  ${catalina.base}/lib/*.jar  JAR文件

  ${catalina.home}/lib  未打包的类和文件

  ${catalina.home}/lib/*.jar  JAR文件

  >  默认情况下，会加载以下内容：

  - *annotations-api.jar* — JavaEE注释类
  - *catalina.jar* — 执行Tomcat的Catalina Servlet容器部分
  - *catalina-ant.jar* — Tomcat Catalina Ant 任务
  - *catalina-ha.jar* — 高可用包
  - *catalina-tribes.jar* — 组通信包
  - *ecj-\*.jar* — Eclipse JDT Java 编译器
  - *el-api.jar* — EL 2.2 API.
  - *jasper.jar* — JSP 运行时编译器
  - *jasper-el.jar* — EL表达式的实现
  - *jsp-api.jar* — JSP 2.2 API.
  - *servlet-api.jar* — Servlet 3.0 API.
  - *tomcat-api.jar* — 由Tomcat定义的几个接口
  - *tomcat-coyote.jar* — Tomcat连接器和使用程序类
  - *tomcat-dbcp.jar* — 基于Apache Commons Pool和Apache Commons DBCP的数据库连接池
  - *tomcat-i18n-\**.jar* — 包含其他语言的资源约束的可选JAR，默认捆绑包含在每个单独的应用中，如果不需要国际化，可以删除
  - *tomcat-jdbc.jar* — Tomcat JDBC数据库连接池
  - *tomcat-util.jar* — Tomcat的各种组件使用的常见类
  - *tomcat7-websocket.jar* — WebSocket 1.1 实现
  - *websocket-api.jar* — WebSocket 1.1 API

  *注：* CATALINA_HOME是Tomcat的安装目录，CATALINA_BASE是Tomcat的工作目录，一个Tomcat可以通过配置CATALINA_BASE来增加多个工作目录，也就是增加多个实例。多个实例各自可以有自己的conf，logs，temp，webapps。

  >  server.loader和shared.loader

  在common.loader加载完毕后，tomcat启动程序会检查catalina.properties文件中配置的server.loader和shared.loader是否设置。如果设置，读取tomcat下对应的server和shared这两个目录的类库。server和shared是对应tomcat目录下的两个目录，在Tomcat中默认是没有，catalina.properties中默认也是没有设置其值。设置方法如下：

  ```properties
  server.loader=${catalina.base}/server/classes,${catalina.base}/server/lib/*.jar
  shared.loader=${catalina.base}/shared/classes,${catalina.base}/shared/lib/*.jar
  ```

  同时需要在tomcat目录下创建server和shared目录结构并将公用的、应用类放到里面。类加载顺序为：

  Bootstrap ---> System ---> /WEB-INF/classes ---> /WEB-INF/lib/*.jar ---> Common ---> Server ---> Shared

- 字符缓存设置

  ```properties
  # String cache configuration.
  tomcat.util.buf.StringCache.byte.enabled=true
  #tomcat.util.buf.StringCache.char.enabled=true
  #tomcat.util.buf.StringCache.trainThreshold=500000
  #tomcat.util.buf.StringCache.cacheSize=5000
  ```

**总结：** Tomcat可以通过catalina.properties的server和shared，为webapp提供公用类库。使一些公用的、不需要与webapp放在一起的设置信息单独保存，在更新webapp的war的时候无需更改webapp的设置。

### 1.2 catalina.policy

包含由Java Security Manager实现的安全策略声明，它替换了安装java时带有的java.policy文件。这个文件用来防止欺骗代码或JSP执行带有像System.exit(0)这样可能影响容器的破坏性代码，只有当Tomcat用-security命令行参数启动时这个文件才会被使用。

### 1.3 context.xml

这个通用context.xml可被所有的web应用程序使用，这个文件默认地可以设置到何处访问各web应用程序中的web.xml文件。context.xml文件的作用和server.xml中<Context>标签作用相同。在tomcat5.5之后，对Context的配置不推荐在server.xml中进行配置，而是在/conf/context.xml中进行独立的配置。因为server.xml是不可动态重加载的资源，服务器一旦启动了以后，要修改这个文件，就得重启服务器才能重新加载。而context.xml文件则不然，tomcat服务器会定时去扫描这个文件。一旦发现文件被修改（时间戳改变了），就会自动重新加载这个文件，而不需要重启服务器。

默认的context.xml如下：

```xml
<Context>
    <WatchedResource>WEB-INF/web.xml</WatchedResource>
    <WatchedResource>${catalina.base}/conf/web.xml</WatchedResource>
</Context>
```

以下给出一个JNDI数据源的配置：

```xml
<Resource name="jdbc/mysql"
        auth="Container"
        type="com.alibaba.druid.pool.DruidDataSource"
        maxActive="100"
        maxIdle="30"
        maxWait="10000"
        username="root"
        password="root"
        driverClassName="com.mysql.jdbc.Driver"
        url="jdbc:mysql://localhost:3306/test"
/>
```

> context.xml的作用范围

- tomcat server级别

  在/conf/context.xml里配置

- Host级别

  在/conf/Catalina/${hostName}里添加context.xml，继而进行配置。

- web app级别

  在/conf/Catalina/\${hostName}里添加\${webappName}.xml，继而进行配置。

### 1.4 server.xml

tomcat的主要配置文件，解析器用这个文件在启动时根据规范创建容器。

默认的server.xml如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Server port="8005" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />
  <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
  <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
  <GlobalNamingResources>
    <Resource name="UserDatabase" auth="Container"
              type="org.apache.catalina.UserDatabase"
              description="User database that can be updated and saved"
              factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
              pathname="conf/tomcat-users.xml" />
  </GlobalNamingResources>
  <Service name="Catalina">
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />
    <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
    <Engine name="Catalina" defaultHost="localhost">
      <Realm className="org.apache.catalina.realm.LockOutRealm">
        <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
               resourceName="UserDatabase"/>
      </Realm>
      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">
        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="localhost_access_log" suffix=".txt"
               pattern="%h %l %u %t &quot;%r&quot; %s %b" />
      </Host>
    </Engine>
  </Service>
</Server>
```

Server是顶级元素，代表一个Tomcat实例。可以包含一个或多个Service，每个Service都有自己的Engines和Connectors。

> Server元素

- className

  使用Java实现类的名称。这个类必须实现org.apache.catalina.Server接口。如果没有指定类名，将会使用标准实现。

- address

  server在这个TCP/IP地址上监听一个shutdown命令。如果没有指定地址，将会使用localhost。

- port

  server在这个端口上监听一个shutdown命令。设置为-1表示禁用shutdown命令。

- shutdown

  连接到指定端口的TCP/IP收到这个命令字符后，将会关闭Tomcat。

> Listeners元素

Server可以包含多个监听器。一个监听器监听指定事件，并对其作出响应。

GlobalResourcesLifecycleListener作用于全局资源，保证JNDI对资源的可达性，比如数据库。

```xml
<Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
```

- SSLEngine

  使用的SSLEngine名称。off：不适用[SSL](https://baike.baidu.com/item/ssl/320778?fr=aladdin)，on：使用[SSL](https://baike.baidu.com/item/ssl/320778?fr=aladdin)但不指定引擎。默认值是on。会初始化本地[SSL](https://baike.baidu.com/item/ssl/320778?fr=aladdin)引擎，对于使用SSLEnabled属性的APR/native connector来讲，该选项必须可用。

- SSLRandomSeed

  指定伪随机数生成器（PRNG）的随机数种子源，默认值为builtin。在开发环境下，可能要将其设置为/dev/urandom，以获得更快的启动速度。

- FIPSMode

  设置为on会请求OpenSSL进入FIPS模式（如果OpenSSL已经处于FIPS模式，将会保留该模式）。该设置为enter会强制OpenSSl进入FIPS模式（如果OpenSSL已经处于FIPS模式，将会产生一个错误）。设置为require要求OpenSSL已经处于FIPS模式（如果OpenSSL当前没有处于FIPS模式将会产生一个错误）。

> GlobalNamingResources元素全局命名资源

GlobalNamingResources元素定义了JNDI（Java命名和目录接口）资源，其允许Java软件客户端通过名称搜寻和查找数据。默认配置定义了一个名称为UserDatabase的JNDI，通过“conf/tomcat-users.xml”得到一个用于用户授权的内存数据库。

```xml
<GlobalNamingResources>
    <Resource name="UserDatabase" auth="Container"
              type="org.apache.catalina.UserDatabase"
              description="User database that can be updated and saved"
              factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
              pathname="conf/tomcat-users.xml" />
  </GlobalNamingResources>
```

也可以定义其他全句话JNDI资源来实现连接池，比如MySQL数据库。

> Services元素

一个Service可以连接一个或多个Connectors到一个引擎。默认配置定义了一个名为“Catalina”的Service，连接了两个Connectors：HTTP和AJP到当前的引擎。

```xml
<Service name="Catalina">
   <Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" />
   <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
   <Engine name="Catalina" defaultHost="localhost">
     <Realm className="org.apache.catalina.realm.LockOutRealm">
       <Realm className="org.apache.catalina.realm.UserDatabaseRealm" 				
              resourceName="UserDatabase"/>
     </Realm>
     <Host name="localhost"  appBase="webapps" unpackWARs="true" autoDeploy="true">
       <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
              prefix="localhost_access_log" suffix=".txt"
              pattern="%h %l %u %t &quot;%r&quot; %s %b" />
     </Host>
   </Engine>
</Service>
```

- className

  该实现使用的Java类名称。这个类必须实现org.apache.catalina.Service接口。如果没有指定类名称，将会使用标准实现。

- name

  Service的显示名称，如果采用了标准的Catalina组件，将会包含日志信息。每个Service与某个特定的Server关联的名称必须是唯一的。

> Connectors元素

一个Connector关联一个TCP端口，负责处理Service与客户端之间的交互。默认配置定义了两个Connectors。

- HTTP/1.1

  处理HTTP请求，使得Tomcat成为一个HTTP服务器。客户端可以通过Connector向服务器发送HTTP请求，接收服务器端的HTTP响应信息。

  ```xml
  <Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" />
  ```

  与生产服务默认使用80端口不同，Tomcat HTTP服务默认在TCP端口8080上运行。可以选择1024到65535之间的任意数字作为端口号来运行Tomcat服务器，前提是该端口没有被任何其他应用使用。connectionTimeOut属性定义了这个connector在链接获得同意之后，获得请求URI line（请求信息）响应的最大等待时间毫秒数。默认为20秒。redirect属性会把[SSL](https://baike.baidu.com/item/ssl/320778?fr=aladdin)请求重定向到TCP的8443端口。


- AJP/1.3

  Apache JServ Protocol connector处理Tomcat服务器与Apache HTTP服务器之间的交互。

  ```xml
  <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
  ```

  可以将Tomcat和Apache HTTP服务运行在一起，Apache HTTP服务器处理静态请求和PHP；Tomcat服务器负责处理Java Servlet/JSP。

> 容器

包含了Engine、Host、Context和Cluster的Tomcat称为容器。最高级的是Engine，最底层的是Context。某些组件，比如Realm和Value，也可以放在容器中。

> Engine引擎

引擎是容器中最高级别的部分。可以包含一个或多个Host。Tomcat服务器可以配置为运行在多个主机名上，包括虚拟主机。

```xml
<Engine name="Catalina" defaultHost="localhost">
```

Catalina引擎从HTTP connector接收HTTP请求，并根据请求头部信息中主机名或IP地址重定向到正确的主机上。

- backgroundProcessorDelay

  这个值表示了在这个引擎和它的子容器上调用backgroundProcess方法之间间隔的秒数，包括所有host和context。值为非负时不会调用子容器（意味着其使用自身的处理线程）。设置为正值会产生一个衍生线程。等待指定的时间之后，该线程会在这个引擎和它的所有子容器上调用backgroundProcess方法。如果没有指定，默认值为10，即会有10秒的延迟。

- className

  实现该引擎使用的Java类名。该类必须实现org.apache.catalina.Engine接口。如果没有指定，会使用标准值。

- defaultHost

  默认主机名，定义了处理指向该服务器的请求所在主机的名称，但名称不是在这个文件中配置。

- jvmRoute

  在负载均衡场景下必须定义该参数，来保证session affinity可用，对于集群中所有Tomcat服务器来讲定义的名称必须是唯一的，该名称将会被添加到生成的会话标示符中，因此，允许前端代理总是将特定会话转发到同一个Tomcat实例。

- name

  Engine的逻辑名称，用在日志和错误信息中。当在相同的Server中使用多个Service元素时，每个Engine必须制定一个唯一的名称。

- startStopThreads

  Engine在启动Host子元素时将会并发使用的线程数。如果设置为0，将会使用Runtime.getRuntime().availableProcessors()的值。设置为负数，将会使用Runtime.getRuntime().availableProcessors() + value的值，如果结果小于1，将会使用1个线程。如果没有指定，默认值为1。

> Realm元素

一个Realm（域）就是一个包含user、password和role认证（比如访问控制）的数据库。你可以在任何容器中定义Realm，例如Engine、Host、Context和Cluster。

```xml
<Realm className="org.apache.catalina.realm.LockOutRealm">
        <Realm className="org.apache.catalina.realm.UserDatabaseRealm" resourceName="UserDatabase"/>
</Realm>
```

默认配置定义了一个Catalina Engine的Realm（UserDatabaseRealm），对用户访问engine的权限进行控制。其使用定义在GlobalNamingResources中，名字为UserDatabase的JNDI。

除了UserDatabaseRealm以外，还有：JDBCRealm（授权用户是否可以通过JDBC驱动连接到关系型数据库）；DataSourceRealm（通过JNDI连接到数据库）；JNDIRealm（连接到一个LDAP目录）；MemoryRealm（将XML文件加载到内存）。

- className

  使用Java实现类的名称。这个类必须实现org.apache.catalina.Realm接口。

> Hosts

一个Host定义了在Engine下的一个虚拟机，反过来其又支持多个Context（web应用）。

```xml
<Host name="localhost"  appBase="webapps" unpackWARs="true" autoDeploy="true">
```

默认配置定义了一个名为localhost的主机。appBase属性定义了所有webapp的根目录，在这种情况下是webapps。默认情况下，每一个webapp的URL和它所在的目录名称相同。例如，默认的Tomcat安装目录的webapps下提供了四个web应用：docs、examples、host-manager和manager。只有ROOT是个例外，它用一个空字符串定义。也就是说，它的URL是http://localhost:8080/。unpackWARs属性指定了放到webapps目录下的WAR-file是否应该被解压。对于unpackWARs=“false”，Tomcat将会直接从WAR-file运行应用，而不解压，这可能导致应用运行变慢。autoDeploy属性指定了是否自动部署放到webapps目录下的应用。

- appBase

  虚拟机应用的根目录。该目录是一个可能包含部署到虚拟机上web应用的路径名。也可能是一个指定的绝对路径名，或者是一个相对于$CATALINA_BASE目录的路径名。如果没有指定，默认会使用webapps。

- xmlBase

  虚拟机XML根目录。该目录是一个可能包含部署到虚拟机上context XML描述符的路径名。也可能是一个指定的绝对路径名，或者是一个相对于$CATALINA_BASE目录的路径名。如果没有指定，默认会使用conf/目录。

- createDirs

  如果设置为true，Tomcat将会在启动阶段，尝试创建一个由appBase和xmlBase属性定义的目录。默认值为true。如果设置为true，并且目录创建失败，将会打印出一个错误信息，但是不会终止启动过程。

- autoDeploy

  该属性的值指明了在Tomcat运行的时候，是否需要定义检查新的或者更新后的web应用。如果为true，Tomcat会定义检查appBase和xmlBase目录，并对找到的新web应用和context XML描述符进行部署。更新web应用或XML上下文描述符将会触发web应用的重载。默认值为true。

- backgroundProcessorDeploy

  表示在调用这台主机的backgroundProcess方法和它的子容器方法，包括所有的context，之间延迟的秒数。如果延迟值不是负数的话，不会调用子容器（意味着会使用它们自己的处理线程）。设置为正数会产生衍生线程。在等待指定的时间之后，线程将会在该host上调用backgroundProcess方法，包括它的所有子容器。host将会使用后台进程执行web应用部署相关的任务。如果没有指定，默认值为-1，意味着host将会依赖于它的父引擎的后台处理线程。

- className

  使用的Java实现类的名称。该类必须实现org.apache.catalina.Host接口。

- deployIgnore

  一个正则表达式，定义了在自动部署和启动时部署的情况下需要忽略的目录。这就允许我们在版本控制系统中保持自己的配置，例如，不会将.svn或者git文件夹部署到appBase目录下。该正则表达式是相对于appBase的。同时也是固定的，意味着是相对于整个文件或目录的名称进行的。因此，foo只会匹配名称为foo的文件或目录，而不会匹配foo.war等名称的文件或目录。如果想让“foo”匹配任意名称，可以使用“.\*foo.\*”。

- deployOnStartup

  指定在Tomcat启动时是否需要自动部署host下的web应用。默认值为true。

- failCtxlfServletStartFails

  设置为true时，如果它的任意一个load-on-startup >= 0的servlet停止自身启动后，停止启动它的每一个子context。每一个子context可能覆盖这个属性。如果没有指定，将会使用默认值false。

- name

  通常是虚拟主机的网络名称，注册在你的域名服务器上。无论指定的主机名称是什么样的，Tomcat在内部都会将其转换为小写。嵌套在Engine内部的Host，其中必须有一个Host的名称匹配Engine的默认Host设置。

- startStopThreads

  Host在启动子Context元素时会并发使用的线程数。如果自动部署被使用的话将会使用该线程池部署新的Context。值为0时将会使用Runtime.getRuntime().availableProcessors()的值。值为负数时将会使用Runtime.getRuntime().availableProcessors()加上该值得和，小于1时将会使用1个线程。如果没有指定，会使用默认值1。

- undeployOldVersion

  该选项的值决定Tomcat，即自动部署进程部分，是否会检查并发部署的过时web应用，任何找到的应用都会被移除。只有在autoDeploy为true的情况下才会生效。如果没有指定将会使用默认值false。

> Value

Value（阀门）作为请求的前置处理程序，可以在请求发送到应用之前拦截HTTP请求。可以定义在任何容器中，比如Engine、Host、Context和Cluster。默认配置中，AccessLogValue会拦截HTTP请求，并在日志文件中创建一个切入点

```xml
<Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="localhost_access_log" suffix=".txt"
               pattern="%h %l %u %t &quot;%r&quot; %s %b" />
```

- className

  设置为org.apache.catalina.ha.tcp.ReplicationValue

- filter

  对于已知文件扩展名或url，可以在请求中使用Value通知cluster没有修改session，对于本次变化cluster没有必要通知session管理者。如果请求匹配该过滤器模型，cluster会假设session没有发生变化。一个filter样例大概是这样的filter=“.\*.gif|.\*.js|.\*.jpeg|.\*.jpg|.\*.png|.\*.htm|.\*.html|.\*.css|.\*.txt”。filter使用java.util.regex的正则表达式。

- primaryIndicator

  布尔值，如果为true，replication value将会把primaryIndicatorName属性定义的名称插入到request属性中，该值无论是Boolean.TRUE或者Boolean.FALSE，都会被放入request属性中。

- primaryIndicatorName

  默认值为org.apache.catalina.ha.tcp.isPrimarySession，这个值定义了一个request属性的名称，值是一个布尔值，表示会话所在的服务器是否为主服务器。

- statistics

  布尔值，如果想让value手机请求的统计数据，设置为true，默认值为false。

- RemoteAddrValue

  阻截来自特定IP地址的请求。

- RemoteHostValue

  阻截基于主机名称的请求。

- RequestDumperValue

  记录了请求的详细信息。

- SingleSignOnValue

  当置于a下时，允许单点登录到该主机下的所有应用上。

  ​

### 1.5 tomcat-users.xml

用于访问tomcat管理应用程序时的安全性设置，用server.xml中引用的默认的用户数据库域（UserDatabase Realm）使用它，所有的凭证默认都是被注释的，如需授权和访问控制，或配置角色，可参考以下配置。

```xml
<role rolename="manager"/>
<role rolename="manager-gui"/>
<role rolename="admin"/>
<role rolename="admin-gui"/>
<user username="admin" password="admin" roles="admin-gui,admin,manager-gui,manager"/>
```

这样tomcat7首页上的Server Status、Manager App、Host Manager就都可以点击登录进去。

tomcat6配置：

```xml
<role rolename="admin"/>
<role rolename="manager"/>
<user username="admin" password="admin" roles="admin,manager"/>
```



### 1.6 web.xml

默认的web.xml文件可被所有的web应用程序使用，这个web.xml文件会设置jspservlet以支持应用程序处理jsps，并设置一个默认的servlet来处理静态资源和html文件，它还设置默认的回话超时以及像index.jsp，index.html这类欢迎文件，并且它为最通用的扩展文件设置默认的[MIME](http://www.w3school.com.cn/media/media_mimeref.asp)类型。

一般在Java工程中，web.xml用来初始化工程配置信息，比如welcome页面，filter，listener，servlet，servlet-mapping，启动加载级别等等。

当应用程序被部署到tomcat时，它会用[engine name]/[host name]/[context-path name].xml创建与context.xml等效的文件，如用户也在\$CATALINA_BASE/conf/[enginename]/[hostname]/context.xml.default文件，在这个文件中特定主机下的所有web应用程序将对主机器虚拟环境采用一系列默认设置。

下面就详细介绍一下web.xml中常用的标签及其功能。

>  \<description\>，\<display-name\>，\<icon\>

- \<description\>

  ```xml
  <description>项目描述</description> <!--对项目作出描述-->
  ```

- \<display-name\>

  ```xml
  <display-name>项目名称</display-name> <!--定义项目的名称-->
  ```

- \<icon\>及\<small-icon\>，\<large-icon\>

  \<icon\> icon元素包含small-icon和large-icon两个子元素，用来指定web站台中小图标和大图标的路径。

  ```xml
  <!--small-icon元素应指向web站台中某个小图标的路径，大小为16X 16 pixel，但是图像文件必须为GIF或JPEG格式，扩展名必须为.git或.jpg-->
  <small-icon>/路径/smallicon.gif</small-icon>
  <!--large-icon元素应指向web站台中某个大图标路径，大小为32X 32pixel，但是图像文件必须为GIF或JPEG的格式，扩展名必须为.git或.jpg-->
  <large-icon>/路径/largeicon.jpg</large-icon>
  ```

  例如：

  ```xml
  <display-name>Demo Example</display-name>
  <description>JSP 2.0 Demo Example</description>
  <icon>
  	<small-icon>/images/small.gif</small-icon>
    	<large-icon>/images/large.gif</large-icon>
  </icon>
  ```

> \<context-param\>

\<context-param\>元素含有一对参数名和参数值，用作应用的servlet上下文初始化参数。参数名在整个web应用中必须是唯一的。

例如：

```xml
<context-param>
	<param-name>name</param-name>
  	<param-value>haha</param-value>
</context-param>
```

此处设定的参数，在JSP页面可以使用\${initParam.name}来获取。

在Servlet中可以使用下列方式获取：

~~~java
String name = getServletContext().getInitParamter("name");
~~~

> \<filter\>

filter元素用于指定web容器中的过滤器。

在请求和响应对象被servlet处理之前或之后，可以使用过滤器对这两个对象进行操作。通过filter-mapping元素，过滤器被映射到一个servlet或一个URL模式。这个过滤器的filter元素和filter-mapping元素必须具有相同的名称。

filter元素用来声明filter的相关设定，filter元素除了下面介绍的子元素之外，还包括<icon\>，\<display-name\>，\<description\>，\<init-param\>，其用途一样。

例如：

```xml
<filter>
	<filter-name>encodingFilter</filter-name>
  	<filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
 	<init-param>
  		<param-name>encoding</param-name>
      	<param-value>UTF-8</param-value>
  	</init-param>
</filter>
```

> \<filter-mapping\>

filter-mapping元素用来声明web应用中的过滤器映射。过滤器可被映射到一个servlet或一个URL模式。将过滤器映射到一个servlet中会造成过滤器作用于servlet上。将过滤器映射到一个URL模式中则可以将过滤器应用于任何资源，只要该资源的URL与URL模式匹配。过滤是按照部署描述符的filter-mapping元素出现的顺序执行的。

filter-mapping元素的两个主要子元素filter-name和url-pattern用来定义Filter对应的URL。还有servlet-name和dispatcher子元素，不是很常用。

特别说明一下dispatcher，设置Filter对应的请求方式，有：REQUEST,INCLUDE,FORWAR,ERROR四种，默认为REQUEST。

例如：

```xml
<filter-mapping>
	<filter-name>encodingFilter</filter-name>
  	<url-pattern>/*</url-pattern>
</filter-mapping>
```

> \<servlet\>

在web.xml中完成一个最常见的任务是对servlet或JSP页面给出名称和定制的URL。用servlet元素分配名称，使用servlet-mapping元素将定制的URL与刚分配的名称相关联。

例如：

```xml
<servlet>
	<servlet-name>dispatcher</servlet-name>
  	<servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
</servlet>
```

> \<servlet-mapping\>

servlet-mapping元素包含两个子元素servlet-name和url-pattern，用来定义servlet所对应的URL。

例如：

```xml
<servlet-mapping>
	<servlet-name>dispatcher</servlet-name>
  	<url-pattern>/*</url-pattern>
</servlet-mapping>
```

> \<listener\>

listener元素用来注册一个监听器类，可以在web应用中包含该类。使用listener元素，可以收到事件什么时候发生以及用什么作为响应的通知。

listener元素用来定义Listener接口，它的主要子元素为\<listener-class\>

例如：

```xml
<listener>
	<listener-class>com.gnd.web.listener.TestListener</listener-class>
</listener>
```

> \<session-config\>

session-config包含一个子元素session-timeout，定义web应用中session的有效期限。

例如：

```xml
<session-config>
	<session-timeout>900</session-timeout>
</session-config>
```

> \<mime-mapping\>

mime-mapping包含两个子元素extension和mime-type，定义某个扩展名和某一MIME Type做对应。

\<extension\>扩展名名称\</extension\>

\<mime-type\>MIME格式\</mime-type\>

例如：

```xml
<mime-mapping>
	<extension>doc</extension>
  	<mime-type>application/vnd.ms-word</mime-type>
</mime-mapping>
<mime-mapping>
	<extension>xls</extension>
  	<mime-type>application/vnd.ms-excel</mime-type>
</mime-mapping>
<mime-mapping>
	<extension>ppt</extension>
  	<mime-type>application/vnd.ms-powerpoint</mime-type>
</mime-mapping>

```

> \<welcome-file-list\>

welcome-file-list包含一个子元素welcome-file，用来定义首页列表。

welcome-file用来指定首页文件名称，服务器会按照设定的顺序来找首页。

例如：

```xml
<welcome-file-list>
	<welcome-file>index.jsp</welcome-file>
  	<welcome-file>index.html</welcome-file>
</welcome-file-list>
```

> \<error-page\>

error-page元素包含三个子元素error-code，exception-type和location。

将错误代码后异常的种类对应到web应用资源路径。

例如：

```xml
<error-page>
	<error-code>404</error-code>
  	<location>error404.jsp</location>
</error-page>
<error-page>
	<exception-type>java.lang.Exception</exception-type>
  	<location>error404.jsp</location>
</error-page>
```

> \<jsp-config\>

jsp-config元素主要用来设定jsp的相关配置，jsp-config包括taglib和jsp-property-group两个子元素，其中taglib元素在JSP1.2时就已经存在，而jsp-property-group是JSP2.0新增的元素。

\<taglib\>

taglib元素包含两个子元素taglib-uri和taglib-location，用来设定JSP网页用到的TagLibrary路径。

\<taglib-uri\>URI\</taglib-uri\>

taglib-uri定义TLD文件的URI，JSP网页的taglib指令可以经由这个URI存取到TLD文件。

\<taglib-location\>/WEB-INF/lib/xxx.tld\</taglib-location\>

TLD文件对应web应用的存放位置。

\<jsp-property-group\>

jsp-property-group元素包含8个子元素，分别为：

\<description\>Description\</description\> 此设定的说明

\<display-name\>Name\</display-name\>  此设定的名称

\<url-pattern\>URL\</url-pattern\>  设定值所影响的范围，如*.jsp

\<el-ignored>true/false\</el-ignored\>  是否支持EL语法

\<scripting-invalid\>true/false\</scripting-invalid\>  是否支持java代码片段<%...%>

\<page-encoding\>UTF-8\</page-encoding\> 设置JSP页面的编码

\<include-prelude\>.jspf\</include-prelude\> 设置JSP页面的抬头，扩展名为.jspf

\<include-coda>.jspf\</include-coda> 设置JSP页面的结尾，扩展名为.jspf

例如：

```xml
<jsp-config>
        <taglib>
            <taglib-uri>Taglib</taglib-uri>
            <taglib-location>/WEB-INF/tlds/MyTaglib.tld</taglib-location>
        </taglib>
        <jsp-property-group>
            <description>Configuration JSP example</description>
            <display-name>JspConfig</display-name>
            <url-pattern>/*</url-pattern>
            <el-ignored>true</el-ignored>
            <page-encoding>UTF-8</page-encoding>
            <scripting-invalid>true</scripting-invalid>
        </jsp-property-group>
    </jsp-config>
```

> \<resource-ref\>

resource-ref元素包含五个子元素description，res-ref-name，res-type，res-auth，res-sharing-scope，利用JNDI取得应用可利用资源。

\<res-auth\>Application/Container\</res-auth\> 资源由Application或Container来许可。

\<res-sharing-scope/>Shareable|Unshareable\<res-sharing-scope/> 资源是否可以共享，默认值为Shareable

例如：

```xml
<resource-ref>
        <res-ref-name>jdbc/Druid</res-ref-name>
        <res-type>com.alibaba.druid.pool.DruidDataSource</res-type>
        <res-auth>Container</res-auth>
    </resource-ref>
```

### 1.7 loggin.properties

JULI记录器使用默认日志配置，它默认地使用ConsoleHandler和fileHandler设置应用程序或者程序包的日志级别。

## 2. 启动流程分析

### 2.1 总体流程说明

在上面对配置文件的说明中，通过server.xml的解释，我们知道server.xml中最顶级的元素是server，而server.xml中的每一个元素我们都可以把它看做是Tomcat中的某一个部分。所以我们可以参照着server.xml来分析源码。

Tomcat最顶层的容器叫Server，它代表着整个Tomcat服务器。Server中至少要包含一个Service来提供服务。Service包含两部分：Connector和Container。Connector负责网络连接，request/response的创建，并对Socket和request、response进行转换等，Container用于封装和管理Servlet，并处理具体的request请求。

一个Tomcat中只有一个Server，一个Server可以有多个Service来提供服务，一个Service只有一个Container，但是可以有多个Connector（一个服务可以有多个连接）。

### 2.2 源码分析

#### 2.2.1 启动总体流程



