package com.carrotsearch.progresso.demos;

import com.carrotsearch.randomizedtesting.annotations.TestGroup;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@TestGroup(enabled = true, sysProperty = "tests.examples")
public @interface Example {}
