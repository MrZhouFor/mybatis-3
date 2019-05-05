package org.apache.ibatis.reflection.custom;

import org.apache.ibatis.reflection.TypeParameterResolver;
import org.junit.jupiter.api.Test;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author zhouze
 * @date 2019-05-05
 */
public class SubClassATest {

    SubClassA<Long> sa = new SubClassA<>();

    @Test
    void testReturn_Lv0SimpleClass() throws Exception {
        Field f = ClassA.class.getDeclaredField("map");
        System.out.println(f.getGenericType());
        System.out.println(f.getGenericType() instanceof ParameterizedType);

        System.out.println("=============================");

        ParameterizedTypeImpl srcType = ParameterizedTypeImpl.make(SubClassA.class, new Type[]{Long.class}, SubClassATest.class);
        Type                  saType  = TypeParameterResolver.resolveFieldType(f, srcType);
        // another method
        //Type sa = TypeParameterResolver.resolveFieldType(f, SubClassATest.class.getDeclaredField("sa").getGenericType());


        System.out.println(saType.getClass());

        System.out.println("*****************************");

        ParameterizedType p = (ParameterizedType) saType;
        System.out.println(p.getRawType());
        System.out.println(p.getOwnerType());

        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

        for (Type t : p.getActualTypeArguments()) {
            System.out.println(t);
        }

    }

}
