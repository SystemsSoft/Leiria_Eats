# Guia: Como Gerar um Android App Bundle (.aab) Assinado

## ⚠️ Informação Importante sobre o Certificado

O arquivo `upload_cert.der` é um **certificado público** exportado do Google Play Console. Este arquivo é útil para validação, mas **não pode ser usado para assinar apps**.

Para assinar um app Android, você precisa de um **keystore** (.jks ou .keystore) que contenha a **chave privada**.

## 📋 Passo a Passo

### 1. Criar um Keystore (se ainda não tiver)

Se você já tem um keystore, pule para o passo 2. Caso contrário, execute o comando abaixo para criar um novo keystore:

```bash
keytool -genkey -v -keystore leiria_eats_keystore.jks -alias leiria_eats -keyalg RSA -keysize 2048 -validity 10000
```

Durante o processo, você será solicitado a fornecer:
- **Senha do keystore**: Crie uma senha forte
- **Senha da chave**: Pode ser a mesma do keystore
- **Nome e Organização**: Informações sobre você ou sua empresa
- **Alias**: Nome para identificar a chave (exemplo: leiria_eats)

⚠️ **IMPORTANTE**: Guarde este keystore e as senhas em um lugar seguro! Se perder, não conseguirá atualizar seu app na Play Store.

### 2. Criar o Arquivo keystore.properties

Copie o arquivo de exemplo e preencha com suas informações:

```bash
cp keystore.properties.example keystore.properties
```

Edite o arquivo `keystore.properties` com seus dados reais:

```properties
storeFile=leiria_eats_keystore.jks
storePassword=SUA_SENHA_DO_KEYSTORE
keyAlias=leiria_eats
keyPassword=SUA_SENHA_DA_CHAVE
```

💡 **Dica**: Se o keystore estiver em outro diretório, use o caminho completo em `storeFile`.

### 3. Gerar o Android App Bundle (.aab)

Execute o comando Gradle para compilar e gerar o .aab assinado:

```bash
./gradlew :composeApp:bundleRelease
```

O arquivo .aab será gerado em:
```
composeApp/build/outputs/bundle/release/composeApp-release.aab
```

### 4. Upload para o Google Play Console

1. Acesse o [Google Play Console](https://play.google.com/console)
2. Selecione seu app
3. Vá em **Produção** > **Criar nova versão**
4. Faça upload do arquivo `composeApp-release.aab`
5. Preencha as informações de lançamento e publique

## 📦 Comandos Úteis

### Limpar e gerar novamente:
```bash
./gradlew clean :composeApp:bundleRelease
```

### Verificar informações do keystore:
```bash
keytool -list -v -keystore leiria_eats_keystore.jks -alias leiria_eats
```

### Gerar APK assinado (para testes):
```bash
./gradlew :composeApp:assembleRelease
```
O APK estará em: `composeApp/build/outputs/apk/release/composeApp-release.apk`

## 🔐 Sobre o Certificado upload_cert.der

O arquivo `upload_cert.der` que você tem é o certificado de upload do Google Play. Ele é usado pelo Google Play para validar que os uploads vêm de você, mas não é usado diretamente no processo de assinatura do app.

### Cenários Comuns:

**Cenário 1: Você Criou o Keystore Original**
- Use o keystore original que você criou
- Siga os passos 2 e 3 acima

**Cenário 2: Google Play Gerencia a Chave de Assinatura do App**
- Você precisa criar um novo keystore de upload
- Use o comando do passo 1 para criar
- O Google Play usa sua própria chave para assinar o app final
- Seu keystore é usado apenas para upload

**Cenário 3: Perdeu o Keystore**
- Se você perdeu o keystore e não usa o App Signing do Google Play, não conseguirá atualizar o app
- Se usa o App Signing do Google Play, você pode criar um novo keystore de upload e registrá-lo no Console

## 🛠️ Resolução de Problemas

### Erro: "keystore.properties não encontrado"
- Certifique-se de criar o arquivo `keystore.properties` na raiz do projeto
- Verifique se o arquivo não tem extensão extra (exemplo: .properties.txt)

### Erro: "KeyStore file does not exist"
- Verifique se o caminho em `storeFile` está correto
- Use caminho absoluto ou relativo à raiz do projeto

### Erro: "Cannot recover key"
- Verifique se o `keyAlias` está correto
- Confirme se a `keyPassword` está correta

## 📝 Versioning

Para atualizar a versão do app antes de gerar o .aab, edite o arquivo `composeApp/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "org.leria.eats.project"
    versionCode = 3      // Incrementar este número a cada release
    versionName = "1.2"  // Versão amigável para usuários
}
```

---

**Versão do App**: 1.2 (versionCode: 3)
**Package**: org.leria.eats.project

