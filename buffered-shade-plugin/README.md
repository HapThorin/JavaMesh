# buffered-shade-plugin

`buffered-shade-plugin`是一个用于在`package`流程中修正目标目录下所有jar包中类的全限定名的插件。

## 内容列表

- [特点介绍](#特点介绍)
- [背景](#背景)
- [使用方式](#使用方式)
- [维护者](#维护者)

## 特点介绍

`buffered-shade-plugin`基于`maven-shade-plugin`插件，使用`asm`字节码修改技术，在打包过程中动态地对目标目录中所有jar包中类的全限定名进行修改。`buffered-shade-plugin`使用`maven-shade-plugin`的轮子，修改逻辑和`maven-shade-plugin`一致。

## 背景

当两个或多个独立工程同时使用一组相同的依赖，而他们又使用`maven-shade-plugin`插件对这些依赖进行重定向的时候，将很难兼容涉及被重定向类的api。因此，我们设计了`buffered-shade-plugin`插件，在其中一个工程打包的过程中，自动将这些重定向的依赖修正至一致，从而达成兼容api的目的。

## 使用方式

在`pom.xml`的`plugins`标签中添加如下代码：

```xml
<plugin>
    <groupId>com.huawei.hwclouds.lubanops</groupId>
    <artifactId>buffered-shade-plugin</artifactId>
    <version>${版本号}</version>
    <executions>
        <execution>
            <id>${id}</id>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <skip>${是否跳过，true/(默认)false}</skip>
        <relocations>
            <relocation>
                <sourceDirectory>${源目录}</sourceDirectory>
                <targetDirectory>${输出目录}</targetDirectory>
                <relocatePatterns>
                    <relocatePattern>
                        <sourcePattern>${源前缀}</sourcePattern>
                        <targetPattern>${目标前缀}</targetPattern>
                    </relocatePattern>
                </relocatePatterns>
                <excludes>
                    <exclude>${被排除的文件名}</exclude>
                </excludes>
            </relocation>
        </relocations>
    </configuration>
</plugin>
```

- `sourceDirectory`为需要修正的jar包目录
- `targetDirectory`为修正后jar包的输出目录
- `sourcePattern`为修正前的前缀，通过`relocatePattern`可添加多组
- `targetPattern`为修正后的前缀，通过`relocatePattern`可添加多组
- `exclude`可通过该参数排除指定jar包，被排除的jar包仅做复制，不进行全限定名修正

## 维护者

[@HapThorin](https://github.com/HapThorin)
