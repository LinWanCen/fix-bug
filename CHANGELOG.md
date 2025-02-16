<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# fix-bug Changelog

## [1.0.0]

- 1.07 Spring injection uses interfaces when there are available
- 1.06 Spring Anno cannot use at `private` member
- 1.05 Maybe need @Primary
- 1.04 Maybe need @Service
- 1.03 Spring Anno cannot use at `static` member
- 1.02 `v = selectAbc()` => `=> if (v == null) { return; }`
- 1.01 `e.printStackTrace();` => `LOG.error("", e);`
- 1.00 Unused Exception => `LOG.error("", e);`

# 中文更新日志

- 1.07 Spring 注入有接口时使用接口
- 1.06 Spring 注解不能使用在 private 成员上
- 1.05 可能需要 @Primary
- 1.04 可能需要 @Service
- 1.03 Spring 注解不能使用在 static 成员上
- 1.02 返回值没判空
- 1.01 输出异常到控制台 改打印日志
- 1.00 吞掉异常 改打印日志