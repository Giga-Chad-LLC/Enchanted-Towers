# Enchanted Towers
## Java Project | Higher School of Economics, 2023

### Project Description:
tbd

### Authors:
1. Vladislav Artiukhov
2. Vladislav Naumkin
3. Dmitrii Artiukhov

### Development Policies:

#### Branch naming:

1. Каждая ветка должна соблюдать следующую конвенцию наименования (разбиение на 3 **сегмента**): `[author-handle]/[topic]/[short-task-description]`, где:

    1. `[author-handle]` - конкатенация **первой буквы имени** и **фамилии** (полностью). Например: `vartiukhov`, `vnaumkin`, `dartiukhov`.

    1. `[topic]` - **тема/категория**, к которой по смыслу относятся изменения в ветке. Например: `feature`, `bug-fix`, `enhancement`. На данный момент допустимо использовать следующие категории (если хотите добавить свои, то обсудите это с членами команды):

        1. `feature`
        1. `bug-fix`
        1. `enhancement`
        1. `research`
        1. `playground`

    1. `[short-task-description]` - краткое описание из 1-6 слов изменений, которые находятся в ветке. Например: `websocket-server-implementation`, `google-map-api-setup`, `drawing-board-implementation`.

1. Для разделения слов в каждом **сегменте** нужно использовать `-`. Например, `vartiukhov/bug-fix/yet-another-branch`.


#### Standard Java Naming Conventions:

1. **Packages:** Names should be in lowercase. With small projects that only have a few packages it's okay to just give them simple (but meaningful!) names:

    ```java
    package pokeranalyzer;
    package mycalculator;
    ```

   In large projects where the packages might be imported into other classes, the names will normally be subdivided. Typically this will start with the company domain before being split into layers or features:

    ```java
    package com.mycompany.utilities;
    package org.bobscompany.application.userinterface;
    ```

1. **Classes:** Names should be in **CamelCase**. Try to use nouns because a class is normally representing something in the real world:

    ```java
    class Customer;
    class Account;
    class WebSocketServer;
    ```

1. **Interfaces:** Names should be in CamelCase starting with letter `I`. They tend to have a name that describes an operation that a class can do:

    ```java
    interface IComparable;
    interface IEnumerable;
    ```

1. **Methods:** Names should be in mixed case. Use verbs to describe what the method does:

    ```java
    void calculateTax();
    String getSurname();
    ```

1. **Variables:** Names should be in mixed case. The names should represent what the value of the variable represents:

    ```java
    String firstName;
    int orderNumber;
    ```

   Only use very **short names** when the variables are short-lived, such as in for loops:

    ```java
    for (int i=0; i < 20; i++) {
        /* `i` only lives in here */
    }
    ```

1. **Constants:** Names should be in uppercase:

    ```java
    static final int DEFAULT_WIDTH;
    static final int MAX_HEIGHT;
    ```
