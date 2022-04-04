# 自动发布项目到 Maven 中央仓库

## 基础的 Maven 配置

参考官方文档：

- https://central.sonatype.org/publish/publish-guide
- https://central.sonatype.org/publish/publish-maven

## 配置 Maven 插件

使用 oss 官方推荐的发布插件：

```xml

<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-deploy-plugin</artifactId>
  <configuration>
    <skip>true</skip>
  </configuration>
</plugin>
<plugin>
<groupId>org.sonatype.plugins</groupId>
<artifactId>nexus-staging-maven-plugin</artifactId>
<executions>
  <execution>
    <id>default-deploy</id>
    <phase>deploy</phase>
    <goals>
      <goal>deploy</goal>
      <goal>release</goal>
    </goals>
  </execution>
</executions>
<configuration>
  <serverId>ossrh</serverId>
  <nexusUrl>https://oss.sonatype.org/</nexusUrl>
</configuration>
</plugin>
```

跳过了默认的 `maven-deploy-plugin` 插件，使用 oss 提供的 `nexus-staging-maven-plugin` 插件。

如果想要手工在 oss 执行 release 操作，就把 `<goal>release</goal>` 去掉，当前的配置会自动执行 `close` 和 `release` 两个操作。

注意配置中的 `<serverId>ossrh</serverId>`，这里的名称是为了能在 GitHub 使用，本地 settings.xml 中如果不是这个名字，可以复制 `server` 加一个这个名字的配置。

## 配置 GitHub Actions

添加 `.github/workflows/maven-deploy.yml` 文件，内容如下：

```yaml
name: Publish package to the Maven Central Repository
on:
  push:
    tags: [ "*" ]
jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up Maven Central Repository
        uses: actions/setup-java@v2
        with:
          java-version: '8'
          distribution: 'adopt'
          server-id: ossrh
          server-username: MAVEN_USERNAME
          server-password: MAVEN_PASSWORD
      - id: install-secret-key
        name: Install gpg secret key
        run: |
          cat <(echo -e "${{ secrets.OSSRH_GPG_SECRET_KEY }}") | gpg --batch --import
          gpg --list-secret-keys --keyid-format LONG
      - name: Publish package
        env:
          MAVEN_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.OSSRH_TOKEN }}
        run: mvn --batch-mode -Dgpg.passphrase=${{ secrets.OSSRH_GPG_SECRET_KEY_PASSWORD }} clean deploy
```

这里的 `on.push.tags` 意思是当推送一个 tag 时，自动执行发布操作。

在 jobs 的具体配置中，用到了下面几个 `Actions secrets` 的配置：

- `OSSRH_USERNAME`: oss 用户名
- `OSSRH_TOKEN`: oss 密码
- `OSSRH_GPG_SECRET_KEY`: gpg 签名密钥
- `OSSRH_GPG_SECRET_KEY_PASSWORD`: gpg 签名密码

这几个配置需要在当前项目的 **Settings** 中配置好。

## gpg 密钥导出和导入

> 基础的配置还参考开头提供的官方文档。
> 参考: https://gist.github.com/sualeh/ae78dc16123899d7942bc38baba5203c

1. 配置 gpg 插件不提示密码:
   ```xml
   <configuration>
     <gpgArguments>
         <arg>--pinentry-mode</arg>
         <arg>loopback</arg>
     </gpgArguments>
   </configuration>
   ```
2. 查找密钥 `gpg --list-secret-keys --keyid-format=long`
3. 导出密钥 `gpg --export-secret-keys -a <key-id> > secret.txt`，不用按参考中替换换行符，用完整的内容即可。
4. 在脚本中导入 `cat <(echo -e "${{ secrets.OSSRH_GPG_SECRET_KEY }}") | gpg --batch --import`。
5. 在 mvn 命令中，通过 `-Dgpg.passphrase=${{ secrets.OSSRH_GPG_SECRET_KEY_PASSWORD }}` 指定密钥的密码。