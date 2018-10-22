package org.ccctc.colleaguedmiclient.annotation;

import org.ccctc.colleaguedmiclient.transaction.data.ViewType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention (RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entity {

    String appl();
    String name();
    String cddName() default "";
    ViewType type() default ViewType.PHYS;
    boolean autoMap() default true;

}
