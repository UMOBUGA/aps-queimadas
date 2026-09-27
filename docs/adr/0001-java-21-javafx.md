# 0001. Java 21 + JavaFX como plataforma única

**Situação:** aceita

## Contexto
A disciplina é de Estrutura de Dados e o enunciado pede um sistema que ordene dados reais e mostre os resultados. O grupo domina Java, e a banca precisa rodar o programa sem instalar servidores.

## Decisão
Um único projeto Maven em Java 21 com três entradas: dashboard JavaFX, menu de console e modos sem interface (`resultados`, `estruturas`, `ml-estudo`) que geram os números da dissertação. Records, `switch` com padrões e text blocks do Java 21 reduzem código de apoio.

## Consequências
- Um JAR executável roda tudo; o CI usa os mesmos modos sem interface.
- JavaFX exige cuidado com a thread da interface: tarefas longas passam por `UiContexto.executar`.
- A distribuição com runtime próprio usa jpackage + jlink (workflow de release).
