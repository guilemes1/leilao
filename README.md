# Leilão de veículos 🚗

O leilão de veículos virtual é um leilão no qual voce pode escolher qual veículo deseja concorrer!

Esse projeto foi desenvolvido com o intuito de colocar em pratica os conhecimentos adquiridos na disciplina de Redes de Computadores, 

---
## Requisitos

As seguintes tecnologias foram utilizadas no desenvolvimento do projeto:

- **[Java 17](https://www.oracle.com/java)** (Ou versões posteriores, dado que o programa utiliza recursos que foram implementados a partir dessa versão)

---

## Como executar:

1. Primeiro execute o comando abaixo para clonar o projeto:

```
git clone https://github.com/guilemes1/leilao.git
```

2. Navegue até a pasta do projeto (Diretório leilao)


3. Execute o comando abaixo para compilar o programa:

```java
javac -cp src -d bin src/*.java
```

4. Em um primeiro terminal, execute o comando abaixo para iniciar o Servidor:

```java
java -cp bin src Servidor
```

5. Em outro terminal, execute o comando abaixo para iniciar o Cliente:

```java
java -cp bin src Cliente
```

// **Obs: Para que o leilão de inicio é necessário mais do que apenas um participante (Cliente), logo é recomendavel rodar mais um cliente em um novo terminal executando o mesmo procedimento.**

6. DIVIRTA-SE 😋