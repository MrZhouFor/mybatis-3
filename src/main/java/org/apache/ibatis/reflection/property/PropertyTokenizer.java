/**
 *    Copyright 2009-2017 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * 实现Iterator接口 可以迭代处理嵌套的多层表达式，例如 'orders[0].items[0].name'
 *
 * @author Clinton Begin
 */
public class PropertyTokenizer implements Iterator<PropertyTokenizer> {
  /**
   * indexedName中 从0到'['位置的字符串
   */
  private String name;
  /**
   * 第一个.之前的字符串
   */
  private final String indexedName;
  /**
   * indexedName中下标数字，'['和']'之间的数字字符
   */
  private String index;
  /**
   * 第一个.之后的字符串
   */
  private final String children;

  public PropertyTokenizer(String fullname) {
    //查找 "." 的位置
    int delim = fullname.indexOf('.');
    if (delim > -1) {
      //name 初始化
      name = fullname.substring(0, delim);
      //children 初始化
      children = fullname.substring(delim + 1);
    } else {
      name = fullname;
      children = null;
    }
    //设置 indexedName
    indexedName = name;
    // '['的位置
    delim = name.indexOf('[');
    if (delim > -1) {
      index = name.substring(delim + 1, name.length() - 1);
      name = name.substring(0, delim);
    }
  }

  public String getName() {
    return name;
  }

  public String getIndex() {
    return index;
  }

  public String getIndexedName() {
    return indexedName;
  }

  public String getChildren() {
    return children;
  }

  @Override
  public boolean hasNext() {
    return children != null;
  }

  @Override
  public PropertyTokenizer next() {
    return new PropertyTokenizer(children);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
  }
}
