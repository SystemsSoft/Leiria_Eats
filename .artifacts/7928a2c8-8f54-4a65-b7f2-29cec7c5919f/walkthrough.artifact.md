# Walkthrough - Adição de Foto de Perfil

Implementei a funcionalidade de adicionar uma foto de perfil na tela de `ProfileScreen`, substituindo o avatar padrão quando uma imagem é selecionada.

## Mudanças Realizadas

### Dados e Persistência
- **[Models.kt](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/composeApp/src/commonMain/kotlin/org/leria/eats/project/data/Models.kt)**: Adicionado campo `photoUrl` à classe `UserProfile`.
- **[ProfileRepository.kt](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/composeApp/src/commonMain/kotlin/org/leria/eats/project/data/ProfileRepository.kt)**: Atualizado para persistir e ler a `photoUrl` do DataStore.
- **[SearchViewModel.kt](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/composeApp/src/commonMain/kotlin/org/leria/eats/project/presentation/viewmodel/SearchViewModel.kt)**: Atualizado o método `updateUserProfile` para incluir a URL da foto.

### UI e Funcionalidade
- **[ProfileScreen.kt](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/composeApp/src/commonMain/kotlin/org/leria/eats/project/presentation/ProfileScreen.kt)**:
    - Adicionado suporte ao **FileKit** para seleção de imagens da galeria.
    - Implementado `KamelImage` para exibir a foto selecionada com recorte circular.
    - Adicionado um ícone de "editar" sobre o avatar para indicar que é clicável.
    - Lógica para salvar a imagem localmente usando `saveImageLocally`.
- **[MainScreenWithAI.kt](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/composeApp/src/commonMain/kotlin/org/leria/eats/project/MainScreenWithAI.kt)**: Conectado o campo `photoUrl` entre a tela de perfil e o ViewModel.

### Dependências
- **[libs.versions.toml](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/gradle/libs.versions.toml)** e **[build.gradle.kts](file:///Users/bruno/Documents/Athenna/Koma/Koma/Leiria_Eats/composeApp/build.gradle.kts)**: Adicionadas dependências do `FileKit` (`compose` e `core`).

## Como Testar
1. Vá para a tela de **Perfil**.
2. Clique no avatar (ícone de usuário).
3. Selecione uma imagem da sua galeria.
4. A imagem deve aparecer no lugar do ícone.
5. Clique em **Guardar Perfil**.
6. Reinicie o app para confirmar que a foto foi salva.
