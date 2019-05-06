/**
 * Copyright 2009-2019 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
import org.apache.ibatis.reflection.wrapper.MapWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * 对属性表达式的解析过程
 *
 * @author Clinton Begin
 */
public class MetaObject {

    /**
     * 原始JavaBean对象
     */
    private final Object               originalObject;
    /**
     * objectWrapper对象，其中封装了originalObject对象
     */
    private final ObjectWrapper        objectWrapper;
    /**
     * 负责实例化originalObject的工厂对象
     */
    private final ObjectFactory        objectFactory;
    /**
     * 负责创建ObjectWrapper的工厂对象
     */
    private final ObjectWrapperFactory objectWrapperFactory;
    /**
     * 用于创建并缓存Reflector对象的工厂对象
     */
    private final ReflectorFactory     reflectorFactory;

    private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
        /**
         * 初始化类成员变量
         */

        this.originalObject = object;
        this.objectFactory = objectFactory;
        this.objectWrapperFactory = objectWrapperFactory;
        this.reflectorFactory = reflectorFactory;

        if (object instanceof ObjectWrapper) {
            //ObjectWrapper 则直接使用
            this.objectWrapper = (ObjectWrapper) object;
        } else if (objectWrapperFactory.hasWrapperFor(object)) {
            //如果objectWrapperFactory能为该原始对象创建对应的ObjectWrapper对象，则优先使用objectWrapperFactory，
            //用户可以自定义ObjectWrapperFactory的实现扩展
            this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
        } else if (object instanceof Map) {
            //如果原始对象为Map，则创建MapWrapper
            this.objectWrapper = new MapWrapper(this, (Map) object);
        } else if (object instanceof Collection) {
            //若原始对象为Collection类型，则创建CollectionWrapper对象
            this.objectWrapper = new CollectionWrapper(this, (Collection) object);
        } else {
            //若原始对象是普通的JavaBean对象，则创建BeanWrapper对象
            this.objectWrapper = new BeanWrapper(this, object);
        }
    }

    /**
     * 创建MetaObject对象的静态方法
     *
     * @param object
     * @param objectFactory
     * @param objectWrapperFactory
     * @param reflectorFactory
     * @return
     */
    public static MetaObject forObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
        if (object == null) {
            //若Object为null，则统一返回 SystemMetaObject.NULL_META_OBJECT 标志
            return SystemMetaObject.NULL_META_OBJECT;
        } else {
            return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
        }
    }

    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    public ObjectWrapperFactory getObjectWrapperFactory() {
        return objectWrapperFactory;
    }

    public ReflectorFactory getReflectorFactory() {
        return reflectorFactory;
    }

    public Object getOriginalObject() {
        return originalObject;
    }

    public String findProperty(String propName, boolean useCamelCaseMapping) {
        return objectWrapper.findProperty(propName, useCamelCaseMapping);
    }

    public String[] getGetterNames() {
        return objectWrapper.getGetterNames();
    }

    public String[] getSetterNames() {
        return objectWrapper.getSetterNames();
    }

    public Class<?> getSetterType(String name) {
        return objectWrapper.getSetterType(name);
    }

    public Class<?> getGetterType(String name) {
        return objectWrapper.getGetterType(name);
    }

    public boolean hasSetter(String name) {
        return objectWrapper.hasSetter(name);
    }

    public boolean hasGetter(String name) {
        return objectWrapper.hasGetter(name);
    }

    public Object getValue(String name) {
        //解析属性表达式
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            //根据PropertyTokenizer对象解析指定的属性，创建相应的MetaObject对象
            MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                return null;
            } else {
                //递归处理子表达式
                return metaValue.getValue(prop.getChildren());
            }
        } else {
            //铜鼓objectWrapper获取指定的属性值
            return objectWrapper.get(prop);
        }
    }

    public void setValue(String name, Object value) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                if (value == null) {
                    // don't instantiate child path if value is null
                    return;
                } else {
                    metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
                }
            }
            metaValue.setValue(prop.getChildren(), value);
        } else {
            objectWrapper.set(prop, value);
        }
    }

    /**
     * 根据PropertyTokenizer对象解析指定的属性，创建相应的MetaObject对象
     *
     * @param name
     * @return
     */
    public MetaObject metaObjectForProperty(String name) {
        Object value = getValue(name);
        return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
    }

    public ObjectWrapper getObjectWrapper() {
        return objectWrapper;
    }

    public boolean isCollection() {
        return objectWrapper.isCollection();
    }

    public void add(Object element) {
        objectWrapper.add(element);
    }

    public <E> void addAll(List<E> list) {
        objectWrapper.addAll(list);
    }

}
