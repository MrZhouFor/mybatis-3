/**
 * Copyright 2009-2018 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 通过Reflector和PropertyTokenizer组合使用，对复杂的属性表达式进行解析,并实现了获取属性描述信息的功能
 *
 * @author Clinton Begin
 */
public class MetaClass {

    /**
     * 缓存reflector对象
     */
    private final ReflectorFactory reflectorFactory;
    /**
     * 记录该类的元数据信息
     */
    private final Reflector        reflector;

    private MetaClass(Class<?> type, ReflectorFactory reflectorFactory) {
        this.reflectorFactory = reflectorFactory;
        this.reflector = reflectorFactory.findForClass(type);
    }

    /**
     * 用于创建MetaClass对象
     *
     * @param type
     * @param reflectorFactory
     * @return
     */
    public static MetaClass forClass(Class<?> type, ReflectorFactory reflectorFactory) {
        return new MetaClass(type, reflectorFactory);
    }

    public MetaClass metaClassForProperty(String name) {
        Class<?> propType = reflector.getGetterType(name);
        return MetaClass.forClass(propType, reflectorFactory);
    }

    public String findProperty(String name) {
        //委托给buildProperty方法实现
        StringBuilder prop = buildProperty(name, new StringBuilder());
        return prop.length() > 0 ? prop.toString() : null;
    }

    public String findProperty(String name, boolean useCamelCaseMapping) {
        if (useCamelCaseMapping) {
            name = name.replace("_", "");
        }
        return findProperty(name);
    }

    public String[] getGetterNames() {
        return reflector.getGetablePropertyNames();
    }

    public String[] getSetterNames() {
        return reflector.getSetablePropertyNames();
    }

    public Class<?> getSetterType(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            MetaClass metaProp = metaClassForProperty(prop.getName());
            return metaProp.getSetterType(prop.getChildren());
        } else {
            return reflector.getSetterType(prop.getName());
        }
    }

    public Class<?> getGetterType(String name) {
        //解析属性表达式
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            MetaClass metaProp = metaClassForProperty(prop);
            //递归调用
            return metaProp.getGetterType(prop.getChildren());
        }
        // issue #506. Resolve the type inside a Collection Object
        return getGetterType(prop);
    }

    private MetaClass metaClassForProperty(PropertyTokenizer prop) {
        //查找指定属性对应的Class
        Class<?> propType = getGetterType(prop);
        //为该属性创建对应的MetaClass对象
        return MetaClass.forClass(propType, reflectorFactory);
    }

    private Class<?> getGetterType(PropertyTokenizer prop) {
        //获取属性类型
        Class<?> type = reflector.getGetterType(prop.getName());
        //表达式是否使用了[] 指定下标，且是collection类型
        if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
            //通过TypeParameterResolver工具解析属性的类型
            Type returnType = getGenericGetterType(prop.getName());
            //针对ParameterizedType进行处理，针对泛型集合进行处理
            if (returnType instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
                if (actualTypeArguments != null && actualTypeArguments.length == 1) {
                    //泛型的类型
                    returnType = actualTypeArguments[0];
                    if (returnType instanceof Class) {
                        type = (Class<?>) returnType;
                    } else if (returnType instanceof ParameterizedType) {
                        type = (Class<?>) ((ParameterizedType) returnType).getRawType();
                    }
                }
            }
        }
        return type;
    }

    private Type getGenericGetterType(String propertyName) {
        try {
            //
            Invoker invoker = reflector.getGetInvoker(propertyName);
            if (invoker instanceof MethodInvoker) {
                Field _method = MethodInvoker.class.getDeclaredField("method");
                _method.setAccessible(true);
                Method method = (Method) _method.get(invoker);
                //返回包装类型
                return TypeParameterResolver.resolveReturnType(method, reflector.getType());
            } else if (invoker instanceof GetFieldInvoker) {
                Field _field = GetFieldInvoker.class.getDeclaredField("field");
                _field.setAccessible(true);
                Field field = (Field) _field.get(invoker);
                return TypeParameterResolver.resolveFieldType(field, reflector.getType());
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return null;
    }

    public boolean hasSetter(String name) {
        //解析属性表达式
        PropertyTokenizer prop = new PropertyTokenizer(name);
        //存在待处理的子表达式
        if (prop.hasNext()) {
            //prop.name 存在指定的setter方法，才继续处理
            if (reflector.hasSetter(prop.getName())) {
                MetaClass metaProp = metaClassForProperty(prop.getName());
                //递归入口
                return metaProp.hasSetter(prop.getChildren());
            } else {
                return false;
            }
        } else {
            //递归出口
            return reflector.hasSetter(prop.getName());
        }
    }

    public boolean hasGetter(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            if (reflector.hasGetter(prop.getName())) {
                MetaClass metaProp = metaClassForProperty(prop);
                return metaProp.hasGetter(prop.getChildren());
            } else {
                return false;
            }
        } else {
            return reflector.hasGetter(prop.getName());
        }
    }

    public Invoker getGetInvoker(String name) {
        return reflector.getGetInvoker(name);
    }

    public Invoker getSetInvoker(String name) {
        return reflector.getSetInvoker(name);
    }

    private StringBuilder buildProperty(String name, StringBuilder builder) {
        //解析属性表达式
        PropertyTokenizer prop = new PropertyTokenizer(name);
        //是否还有子表达式
        if (prop.hasNext()) {
            //查找prop.name对应的属性
            String propertyName = reflector.findPropertyName(prop.getName());
            if (propertyName != null) {
                //追加属性名
                builder.append(propertyName);
                builder.append(".");
                //为该属性创建对应的MetaClass对象
                MetaClass metaProp = metaClassForProperty(propertyName);
                //递归解析prop.children字段
                metaProp.buildProperty(prop.getChildren(), builder);
            }
        } else {
            //递归出口
            String propertyName = reflector.findPropertyName(name);
            if (propertyName != null) {
                builder.append(propertyName);
            }
        }
        return builder;
    }

    public boolean hasDefaultConstructor() {
        return reflector.hasDefaultConstructor();
    }

}
