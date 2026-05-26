
package com.minimall.minimall;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.rules.DateType;

public class CodeGenerator {

    public static void main(String[] args) {

        // ⚠️ URL 一行写完，不要换行！
        String url = "jdbc:mysql://localhost:3306/mini_mall?useUnicode=true&characterEncoding=utf-8& useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "123456";
        String projectPath = System.getProperty("user.dir");

        FastAutoGenerator.create(url, username, password)

                .globalConfig(builder -> {
                    builder.author("liyuq")
                            .outputDir(projectPath + "/src/main/java")
                            .dateType(DateType.TIME_PACK)
                            .disableOpenDir();
                })

                .packageConfig(builder -> {
                    builder.parent("com.minimall.minimall")
                            .entity("entity")
                            .mapper("mapper")
                            .service("service")
                            .serviceImpl("service.impl")
                            .controller("controller");
                })

                .strategyConfig(builder -> {
                    builder.addInclude("user", "category", "product", "address",
                                    "cart_item", "orders", "order_item")
                            .entityBuilder()
                            .enableLombok()
                            .logicDeleteColumnName("is_deleted")
                            .mapperBuilder()
                            .enableMapperAnnotation()
                            .controllerBuilder()
                            .enableRestStyle();
                })

                .execute();

        System.out.println("🎉 生成完成！");
    }}
      