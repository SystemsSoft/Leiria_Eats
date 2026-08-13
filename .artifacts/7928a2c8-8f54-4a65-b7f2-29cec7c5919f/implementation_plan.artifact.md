# Adicionar Foto de Perfil

Ajustar a tela de perfil para permitir que o usuário adicione uma foto no lugar do avatar padrão.

## Mudanças Propostas

### [Componente de Dados]

#### [MODIFICAR] [Models.kt](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/composeApp/src/commonMain/kotlin/org/leria/eats/project/data/Models.kt)
- Adicionar `photoUrl: String? = null` à data class `UserProfile`.

#### [MODIFICAR] [ProfileRepository.kt](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/composeApp/src/commonMain/kotlin/org/leria/eats/project/data/ProfileRepository.kt)
- Adicionar chave para `photoUrl` no DataStore.
- Atualizar `userProfileFlow` para ler a `photoUrl`.
- Atualizar `saveProfile` para salvar a `photoUrl`.

### [Componente de Apresentação]

#### [MODIFICAR] [SearchViewModel.kt](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/composeApp/src/commonMain/kotlin/org/leria/eats/project/presentation/viewmodel/SearchViewModel.kt)
- Atualizar `updateUserProfile` para aceitar e passar a `photoUrl`.

#### [MODIFICAR] [ProfileScreen.kt](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/composeApp/src/commonMain/kotlin/org/leria/eats/project/presentation/ProfileScreen.kt)
- Atualizar a assinatura da função `ProfileScreen` para incluir `onSave` com `photoUrl`.
- Adicionar estado local para `photoUrl`.
- Substituir o ícone de avatar por um componente que exibe a imagem (usando `KamelImage`) ou o ícone se estiver vazio.
- Adicionar funcionalidade de clique no avatar para selecionar uma imagem usando `FileKit`.
- Adicionar dependência do `FileKit` se necessário (verificarei se o projeto compila, pois já existem referências no código).

#### [MODIFICAR] [MainScreenWithAI.kt](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/composeApp/src/commonMain/kotlin/org/leria/eats/project/MainScreenWithAI.kt)
- Atualizar a chamada do `ProfileScreen` para passar e receber a `photoUrl`.

## Plano de Verificação

### Verificação Manual
- Abrir a tela de perfil.
- Clicar no avatar.
- Selecionar uma foto da galeria.
- Verificar se a foto é exibida no lugar do avatar.
- Salvar o perfil.
- Fechar e abrir o app para garantir que a foto foi persistida.
