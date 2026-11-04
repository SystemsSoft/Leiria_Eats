# Guia: Como Publicar o App iOS (KomaAi) na App Store

## ⚠️ Antes de Começar

- **O projeto precisa compilar a partir do git.** Hoje várias pastas do `composeApp` (`domain/`, `data/remote/`, `data/repository/`, `voice/live/`, etc.) existem só como arquivos não rastreados no seu Mac. Faça commit delas antes de publicar, senão ninguém consegue compilar o app a partir do repositório.
- **Conta Apple Developer** (US$ 99/ano): https://developer.apple.com/programs/enroll/ — sem ela não é possível assinar nem enviar o app.
- **Xcode** instalado (já está neste Mac) e logado com a conta: Xcode > Settings > Accounts > `+` > Apple ID.

## 📋 Dados do App (já configurados)

| Item | Valor | Onde fica |
|---|---|---|
| Nome na tela inicial | `KomaAi` | `iosApp/Configuration/Config.xcconfig` |
| Bundle ID | `org.leria.eats.project` (igual ao Android) | `iosApp/Configuration/Config.xcconfig` |
| Versão | `2.3` (build `14`) — acompanhar o `versionName`/`versionCode` do Android | `iosApp/Configuration/Config.xcconfig` |
| Dispositivos | Somente iPhone | `project.pbxproj` (`TARGETED_DEVICE_FAMILY = 1`) |
| iOS mínimo | 18.2 | `project.pbxproj` |
| Ícone | `app-icon-1024.png` (1024×1024, sem transparência) | `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/` |
| Pagamento | Stripe PaymentSheet (Swift Package `stripe-ios-spm`) | `iosApp/iosApp/StripeBridge.swift` |

## 📋 Passo a Passo

### 1. Preencher o Team ID

Pegue o **Team ID** (10 caracteres) em https://developer.apple.com/account → Membership details e preencha em `iosApp/Configuration/Config.xcconfig`:

```properties
TEAM_ID=ABCDE12345
```

### 2. Registrar o Bundle ID

Em https://developer.apple.com/account/resources/identifiers → `+` → **App IDs** → **App**:
- **Description**: KomaAi
- **Bundle ID (Explicit)**: `org.leria.eats.project`
- Capabilities: nenhuma extra é necessária.

💡 Com "Automatically manage signing" (já ativo no projeto) o Xcode também consegue criar isso sozinho no passo 4.

### 3. Criar o App no App Store Connect

Em https://appstoreconnect.apple.com → **Apps** → `+` → **New App**:
- **Platform**: iOS
- **Name**: KomaAi (nome na loja; precisa estar livre)
- **Primary Language**: Portuguese (Portugal)
- **Bundle ID**: `org.leria.eats.project`
- **SKU**: qualquer identificador interno, ex.: `komaai-ios`

### 4. Gerar o Archive no Xcode

```bash
open iosApp/iosApp.xcodeproj
```

1. Na barra superior, selecione o esquema **iosApp** e o destino **Any iOS Device (arm64)**.
2. Em **Signing & Capabilities**, confirme que o Team aparece e não há erros de assinatura.
3. Menu **Product > Archive** (a primeira vez demora: compila o Kotlin em Release e baixa o SDK da Stripe).
4. Ao terminar, abre o **Organizer** com o archive.

### 5. Enviar para o App Store Connect

No Organizer: **Distribute App** → **App Store Connect** → **Upload** → manter as opções padrão → **Upload**.

Depois de alguns minutos o build aparece em App Store Connect > TestFlight.

💡 **Teste pelo TestFlight antes de enviar para revisão**, principalmente: pedido por voz (microfone), localização e um pagamento real com a Stripe — nada disso foi testado em iPhone físico ainda.

### 6. Preencher a Ficha da Loja

Em App Store Connect > o app:
- **Screenshots**: obrigatórias para iPhone 6,9" (ex.: 1320×2868). Podem ser tiradas no simulador iPhone 17 Pro Max.
- **Descrição, palavras-chave, URL de suporte**.
- **URL da Política de Privacidade**: obrigatória.
- **App Privacy** (questionário de dados coletados): declarar pelo menos nome, telefone/endereço de entrega, localização precisa, áudio (pedidos por voz), histórico de compras, e informações de pagamento (via Stripe).
- **Age Rating**: responder o questionário.
- **App Review Information**: explicar como testar (ex.: que o app funciona em Leiria, como fazer um pedido de teste). Se o revisor não conseguir fazer um pedido, o app pode ser rejeitado.

### 7. Enviar para Revisão

Selecione o build enviado no passo 5, clique em **Add for Review** → **Submit to App Review**. A revisão costuma levar de 1 a 3 dias.

## 🔄 Próximas Versões

1. Aumente `MARKETING_VERSION` e `CURRENT_PROJECT_VERSION` em `iosApp/Configuration/Config.xcconfig` (o build precisa ser sempre maior que o anterior).
2. Repita os passos 4, 5 e 7.
