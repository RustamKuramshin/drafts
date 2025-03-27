# Quiz: Java Core & JVM (Senior Level)

## Вопрос 1. Java Memory Model – `final` поля
Рассмотрим класс:

```java
class Config {
    final int size;
    Config(int size) { this.size = size; }
}
```

В потоко-небезопасном коде один поток создаёт объект `Config` и присваивает его ссылку общей переменной, а другой почти сразу читает эту переменную. Предположим, что отсутствует синхронизация или volatile. Какие утверждения о таком сценарии верны согласно JMM?

**Варианты ответов:**

- **A.** Другой поток **гарантированно** увидит установленное значение `size` (например, 42) в новом объекте, даже без синхронизации.
- **B.** Другой поток может увидеть значение поля `size` по умолчанию (0) вместо установленного значения.
- **C.** Ключевое слово `final` влияет только на неизменяемость внутри потока и не имеет специальных гарантий видимости между потоками.
- **D.** JMM обеспечивает _безопасную публикацию_ объектов через `final` поля: правильно построенный объект с `final` полями виден другим потокам полностью инициализированным ([Java final & Safe Publication - Stack Overflow](https://stackoverflow.com/questions/50006765/java-final-safe-publication#:~:text=This%20is%20the%20whole%20point%2C,in%20Java%20Concurrency%20in%20Practice)).

**Правильные ответы:** A, D. (JMM гарантирует, что запись в `final` поле в конструкторе “замораживается” при завершении конструктора, поэтому другие потоки не увидят default-значения ([Java final & Safe Publication - Stack Overflow](https://stackoverflow.com/questions/50006765/java-final-safe-publication#:~:text=This%20is%20the%20whole%20point%2C,in%20Java%20Concurrency%20in%20Practice)). Без `final` возможна ситуация, когда другой поток увидит неинициализированное значение.)

## Вопрос 2. `synchronized`: уровень метода vs. блока и статический vs. нестатический
Имеется класс:

```java
class Counter {
    private int count = 0;
    public static synchronized void incStatic() { /* ... */ }
    public synchronized void inc() { /* ... */ }
    public void incBlock() {
        synchronized(this) { count++; }
    }
}
```

Два разных потока вызывают методы этого класса: один выполняет `Counter.incStatic()`, а другой – `counter.inc()` на одном и том же экземпляре `counter`. Какое из нижеперечисленного верно?

**Варианты ответов:**

- **A.** Между вызовами `incStatic()` и `inc()` **может произойти гонка**, т.к. они синхронизируются на разных объектах.
- **B.** Метод `incBlock()` эквивалентен по синхронизации методу `inc()` (оба блокируют монитор текущего экземпляра).
- **C.** Одновременный вызов `incStatic()` и `inc()` **не возможен**, так как оба метода синхронизированы и блокируют друг друга.
- **D.** Статический синхронизированный метод блокирует монитор класса `Counter`, поэтому не мешает потоку, выполняющему нестатический синхронизированный метод на конкретном объекте.

**Правильный ответ:** A (Статический `synchronized` метод использует монитор класса `Counter`, тогда как нестатический – монитор конкретного объекта ([Chapter 17. Threads and Locks](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#:~:text=match%20at%20L645%20,subsequent%20lock%20on%20that%20monitor)) ([Chapter 17. Threads and Locks](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#:~:text=,subsequent%20lock%20on%20that%20monitor)). Поэтому `incStatic()` и `inc()` не блокируют друг друга, что может привести к состояниям гонки. Метод `incBlock()` синхронизируется на `this`, эквивалентно нестатическому `synchronized`-методу, утверждение B верно, но вопрос акцентируется на A.)

## Вопрос 3. Перегрузка: varargs vs. boxing
Что напечатает следующий код?

```java
public class OverloadDemo {
    static void print(Integer i) { System.out.println("Integer"); }
    static void print(int... nums) { System.out.println("int varargs"); }
    public static void main(String[] args) {
        print(5);
    }
}
```

**Варианты ответов:**

- **A.** `Integer`
- **B.** `int varargs`
- **C.** Код не скомпилируется из-за неоднозначности вызова.
- **D.** `Integer` `int varargs` (вывод двух строк).

**Правильный ответ:** A. (Вызывается перегрузка с `Integer`. При перегрузке Java выбирает наиболее специфичный метод: здесь метод с boxing более специфичен, чем varargs. Varargs-метод рассматривается в последнюю очередь ([java - Why does type-promotion take precedence over varargs for overloaded methods - Stack Overflow](https://stackoverflow.com/questions/30573532/why-does-type-promotion-take-precedence-over-varargs-for-overloaded-methods#:~:text=%3E%20%20%20,overloading%20to%20be%20combined%20with)) ([How is ambiguous overloaded method call resolved in java?](https://javabypatel.blogspot.com/2016/05/ambiguous-method-overloading.html#:~:text=How%20is%20ambiguous%20overloaded%20method,Varargs)), поэтому вывод – `Integer`.)

## Вопрос 4. Переопределение vs. перегрузка методов
Что напечатает следующий код?

```java
class A {
    void test(Number n) { System.out.println("Number"); }
}
class B extends A {
    void test(Integer i) { System.out.println("Integer"); }
}
public class OverrideDemo {
    public static void main(String[] args) {
        A obj = new B();
        obj.test(Integer.valueOf(5));
    }
}
```

**Варианты ответов:**

- **A.** `Integer`
- **B.** `Number`
- **C.** Код не скомпилируется из-за некорректного переопределения.
- **D.** Будет выброшено исключение `NullPointerException` во время выполнения.

**Правильный ответ:** B. (Метод `test(Integer)` в классе B **не переопределяет**, а **перегружает** метод из A, поскольку сигнатура отличается. Вызов `obj.test(...)` связывается по типу ссылки `A` – то есть вызывается версия из A, печатающая `Number`. Это иллюстрирует отличие перегрузки от полиморфного переопределения.)

## Вопрос 5. Правила переопределения методов
Какие из утверждений о переопределении методов в Java верны?

**Варианты ответов:**

- **A.** Метод-переопределение в подклассе не может бросать проверяемое исключение шире, чем объявлено в базовом методе ([Why is the finalize() method deprecated in Java 9? - Stack Overflow](https://stackoverflow.com/questions/56139760/why-is-the-finalize-method-deprecated-in-java-9#:~:text=,resurrected)).
- **B.** Метод-переопределение может иметь более узкую область видимости (например, заменить `public` на `protected`).
- **C.** Возвращаемый тип переопределённого метода может отличаться от оригинального, если он является подклассом исходного (ковариантный возвращаемый тип).
- **D.** Статический метод невозможно переопределить в подклассе – при совпадении сигнатуры метод подкласса будет скрывать (hide) метод суперкласса.

**Правильные ответы:** A, C, D. (Переопределённый метод **не может** ужесточить исключения или понизить видимость доступа ([java - Why does type-promotion take precedence over varargs for overloaded methods - Stack Overflow](https://stackoverflow.com/questions/30573532/why-does-type-promotion-take-precedence-over-varargs-for-overloaded-methods#:~:text=%3E%20%20%20,1%29%20can%20change%20the)). Ковариантный возвращаемый тип разрешён с Java 5. Статические методы не участвуют в динамическом распределении – совпадение имени ведёт к сокрытию, а не к полиморфному переопределению.)

## Вопрос 6. Отношение _happens-before_
Какие из следующих пар событий образуют отношение *"происходит-перед"* (happens-before) согласно Java Memory Model?

**Варианты ответов:**

- **A.** Запись в volatile-поле в одном потоке и последующее чтение того же volatile-поля в другом потоке ([Chapter 17. Threads and Locks](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#:~:text=,subsequent%20lock%20on%20that%20monitor)).
- **B.** Вызов `Thread.start()` для потока X и начало выполнения метода `run()` потока X ([Chapter 17. Threads and Locks](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#:~:text=match%20at%20L651%20,actions%20in%20the%20started%20thread)).
- **C.** Выход из блока синхронизированного метода и вход в **любой** другой синхронизированный метод (даже на другом мониторе) в другом потоке.
- **D.** Завершение (`Thread.join()`) потока X в одном потоке и нормальное окончание (`return` из `run`) потока X.

**Правильные ответы:** A, B, D. (Правила JMM: разблокировка монитора происходит-перед последующей блокировкой на том же мониторе; запись в `volatile` – перед последующим чтением этого же `volatile`; запуск потока – перед действиями в нём; завершение всех действий в потоке – перед успешным возвращением из `Thread.join` ([Chapter 17. Threads and Locks](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#:~:text=,subsequent%20lock%20on%20that%20monitor)) ([Chapter 17. Threads and Locks](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#:~:text=,actions%20in%20the%20started%20thread)). Утверждение C неверно: должно быть один и тот же монитор.)

## Вопрос 7. Ключевое слово `volatile`
Выберите верные утверждения о модификаторе `volatile` в Java:

**Варианты ответов:**

- **A.** Чтение/запись volatile-поля устанавливает *видимость* изменений для других потоков, но не гарантирует атомарность составных операций ([Java Concurrency: Part 1 - Quizgecko](https://quizgecko.com/learn/java-concurrency-synchronization-concepts-opkfrz#:~:text=Java%20Concurrency%3A%20Part%201%20,Atomic%20variables)).
- **B.** После Java 5 чтения и записи любых `long` и `double` атомарны, даже без volatile ([[PDF] Final Field Semantics - UMD Computer Science](http://www.cs.umd.edu/~pugh/java/memoryModel/newFinal.pdf#:~:text=Final%20fields%20are%20fields%20that,are%20somewhat%20different%20from)).
- **C.** Запись в volatile-поле **гарантирует** занесение изменений в основную память до выполнения следующих операций (introduces a memory barrier).
- **D.** Использование volatile-полей предотвращает *все* возможные состояния гонки в многопоточной программе.

**Правильные ответы:** A, B, C. (`volatile` обеспечивает *упорядочение* и *видимость* (happens-before) для других потоков ([Chapter 17. Threads and Locks](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#:~:text=,before%20every)), но не делает, например, `x++` атомарным. Неструктурированные гонки всё равно возможны. Для 64-битных типов Java Memory Model гарантирует атомарность их простых чтений/записей начиная с JDK 5, даже без volatile ([[PDF] Final Field Semantics - UMD Computer Science](http://www.cs.umd.edu/~pugh/java/memoryModel/newFinal.pdf#:~:text=Final%20fields%20are%20fields%20that,are%20somewhat%20different%20from)).)

## Вопрос 8. Потокобезопасное инкрементирование счётчика
Имеется общий целочисленный счётчик `counter`, к которому обращаются несколько потоков. Какие способы **гарантированно** обеспечат корректное (без гонок) увеличение `counter` на 1?

**Варианты ответов:**

- **A.** Объявить `counter` как `volatile` и выполнять в потоках операцию `counter++`.
- **B.** Использовать `java.util.concurrent.atomic.AtomicInteger` и вызывать `counter.incrementAndGet()`.
- **C.** Синхронизировать блок, в котором происходит `counter++`, например, `synchronized(lock) { counter++; }`.
- **D.** Использовать класс `LongAdder` (если допускается 64-битный счётчик) из пакета `java.util.concurrent` вместо `AtomicInteger`.

**Правильные ответы:** B, C, D. (Операция `counter++` не атомарна – вариант A **не** потокобезопасен ([Java Concurrency: Part 1 - Quizgecko](https://quizgecko.com/learn/java-concurrency-synchronization-concepts-opkfrz#:~:text=Java%20Concurrency%3A%20Part%201%20,Atomic%20variables)). AtomicInteger обеспечивает атомарность за счёт CAS. Блокировка через `synchronized` также предотвращает одновременный доступ. `LongAdder` – потокобезопасная альтернатива AtomicInteger для счётчиков с высокой конкуренцией. Volatile же только гарантирует видимость, но не атомарность инкремента.)

## Вопрос 9. Избежание deadlock (взаимной блокировки)
Два потока пытаются захватить два разных монитора в обратном порядке, что приводит к deadlock. Какой подход наиболее эффективно предотвратит взаимную блокировку в таком сценарии?

**Варианты ответов:**

- **A.** Всегда захватывать несколько блокировок в одном и том же порядке во всех потоках.
- **B.** Использовать метод `Thread.yield()` перед захватом второй блокировки.
- **C.** Пометить оба монитора ключевым словом `volatile`.
- **D.** Применить структуру `wait()`/`notify()` внутри блока `synchronized` вместо вложенного захвата двух мониторов.

**Правильный ответ:** A. (Наиболее надёжный способ избежать deadlock – **соблюдать порядок захвата** ресурсов ([Java Concurrency Quiz: Can You Master the Multithreading Mayhem? | by Vikrant Dheer | Mar, 2025 | Medium](https://medium.com/@vikrantdheer/java-concurrency-quiz-can-you-master-the-multithreading-mayhem-248c3b6881d0#:~:text=class%20Task%20implements%20Runnable%20,lock1%3B%20private%20final%20Object%20lock2)) ([Java Concurrency Quiz: Can You Master the Multithreading Mayhem? | by Vikrant Dheer | Mar, 2025 | Medium](https://medium.com/@vikrantdheer/java-concurrency-quiz-can-you-master-the-multithreading-mayhem-248c3b6881d0#:~:text=%E2%9C%85%20Options%3A%20A%29%20Use%20,keyword%20for%20both%20lock%20objects)). Использование `tryLock` с таймаутом могло бы обнаружить и смягчить deadlock, но среди приведённых вариантов A – явное правильное решение. `yield()` и volatile не решают проблему, а `wait/notify` предназначены для другого – организации ожидания условий.)

## Вопрос 10. `ReentrantLock` vs. `synchronized`
Выберите утверждения, корректно описывающие различия между `ReentrantLock` (java.util.concurrent.locks) и встроенной синхронизацией (`synchronized`):

**Варианты ответов:**

- **A.** `ReentrantLock` позволяет попытаться захватить блокировку без ожидания (методом `tryLock()`), тогда как `synchronized` всегда блокируется, ожидая монитор ([
  What is ReentrantLock in Java? Difference between synchronized vs ReentrantLock with Example
  ](https://javarevisited.blogspot.com/2013/03/reentrantlock-example-in-java-synchronized-difference-vs-lock.html#:~:text=the%20ability%20to%20trying%20to,in%20Java)) ([
  What is ReentrantLock in Java? Difference between synchronized vs ReentrantLock with Example
  ](https://javarevisited.blogspot.com/2013/03/reentrantlock-example-in-java-synchronized-difference-vs-lock.html#:~:text=2,in%20Java%20applications)).
- **B.** В отличие от `synchronized`, у `ReentrantLock` нет механизма автоматического освобождения – разработчик должен явно вызвать `unlock()` в finally-блоке.
- **C.** `ReentrantLock` поддерживает опциональную *справедливость* (fairness) очереди потоков, а у `synchronized` порядок захвата неопределённый ([
  What is ReentrantLock in Java? Difference between synchronized vs ReentrantLock with Example
  ](https://javarevisited.blogspot.com/2013/03/reentrantlock-example-in-java-synchronized-difference-vs-lock.html#:~:text=1,thread%2C%20in%20case%20of%20contention)).
- **D.** Внутри блока `synchronized` поток может повторно захватывать тот же монитор (реентрантность), тогда как `ReentrantLock` не позволяет одному потоку захватить замок более одного раза.

**Правильные ответы:** A, B, C. (`ReentrantLock` предоставляет дополнительные возможности: ненаправленное или прерываемое ожидание, *fair* режим ([
What is ReentrantLock in Java? Difference between synchronized vs ReentrantLock with Example
](https://javarevisited.blogspot.com/2013/03/reentrantlock-example-in-java-synchronized-difference-vs-lock.html#:~:text=1,thread%2C%20in%20case%20of%20contention)), несколько Condition-объектов. При этом `lock()`/`unlock()` нужно вызывать вручную, что требует аккуратности. Оба механизма являются **реентрантными** – поток может повторно входить в уже захваченную им блокировку; D неверно.)

## Вопрос 11. Алгоритмы сборки мусора (GC)
Какие утверждения о сборщиках мусора HotSpot JVM верны?

**Варианты ответов:**

- **A.** **Serial GC** выполняет сборку мусора в одном потоке и обычно подходит для приложений с небольшими кучами.
- **B.** **Parallel GC** стремится максимизировать пропускную способность (throughput) приложения, используя несколько потоков для сборки ([15 tricky Java Interview Questions & Answers - TestGorilla](https://www.testgorilla.com/blog/tricky-java-interview-questions-answers/#:~:text=1,applications%20with%20low%20memory%20footprints)).
- **C.** **CMS (Concurrent Mark-Sweep)** сборщик полностью заменён G1 GC начиная с Java 11, и в современных версиях JVM CMS недоступен.
- **D.** **G1 GC (Garbage-First)** делит кучу на регионы, собирая их выборочно; он разработан для предсказуемых пауз и стал GC по умолчанию в Java 9+ ([15 tricky Java Interview Questions & Answers - TestGorilla](https://www.testgorilla.com/blog/tricky-java-interview-questions-answers/#:~:text=4.%20Garbage,that%20contain%20the%20most%20garbage)).

**Правильные ответы:** A, B, D. (Serial GC – однопоточный, используется для небольших heaps. Parallel GC – многопоточный, оптимизирует throughput ([15 tricky Java Interview Questions & Answers - TestGorilla](https://www.testgorilla.com/blog/tricky-java-interview-questions-answers/#:~:text=1,applications%20with%20low%20memory%20footprints)). G1 с region-based алгоритмом стал *default* с Java 9, обеспечивая баланс между паузами и скоростью ([15 tricky Java Interview Questions & Answers - TestGorilla](https://www.testgorilla.com/blog/tricky-java-interview-questions-answers/#:~:text=4.%20Garbage,that%20contain%20the%20most%20garbage)). CMS был объявлен устаревшим и удалён в Java 14, уступив место G1, но в Java 11 ещё мог быть включён флагом.)

## Вопрос 12. Ссылки в Java: Soft, Weak, Phantom
Относительно типов ссылок (java.lang.ref) выберите верные утверждения:

**Варианты ответов:**

- **A.** Объект, на который есть только **WeakReference**, будет удалён сборщиком мусора при ближайшей сборке, вне зависимости от наличия памяти ([When Does Java Throw the ExceptionInInitializerError? | Baeldung](https://www.baeldung.com/java-exceptionininitializererror#:~:text=The%20ExceptionInInitializerError%20indicates%20that%20an,when%20we%20see%20this)).
- **B.** **SoftReference** позволяют реализовать кэш: объект с такой ссылкой остается доступным до тех пор, пока в JVM достаточно памяти (при дефиците памяти soft-ссылки очищаются).
- **C.** **PhantomReference** используется вместе с очередью ссылок (ReferenceQueue) для отслеживания завершения жизни объекта; в отличие от finalize, phantom-ссылка не даёт доступа к объекту и вызывается *после* финализации ([ExceptionInInitializerError (Java Platform SE 7 ) - Oracle Help Center](https://docs.oracle.com/javase/7/docs/api/java/lang/ExceptionInInitializerError.html#:~:text=Center%20docs,initializer%20for%20a%20static)).
- **D.** Сильная (strong) ссылка отличается от перечисленных тем, что *никогда* не собирается мусором.

**Правильные ответы:** A, B, C. (WeakReference указывает сборщику мусора, что объект можно собрать при первой же возможности ([When Does Java Throw the ExceptionInInitializerError? | Baeldung](https://www.baeldung.com/java-exceptionininitializererror#:~:text=The%20ExceptionInInitializerError%20indicates%20that%20an,when%20we%20see%20this)). SoftReference очищается только при нехватке памяти, поэтому используется в кэшах. PhantomReference – самая слабая, нужна для уведомления, когда объект реально удалён из памяти; она требует ReferenceQueue и используется, например, для освобождения ресурсов вне heap. Сильные ссылки собираются, когда на объект нет ни одной сильной ссылки; D неверно.)

## Вопрос 13. **Sealed** классы и интерфейсы (Java 17)
Какие утверждения о *запечатанных* (sealed) классах/интерфейсах в Java 17 верны?

**Варианты ответов:**

- **A.** Sealed-класс должен явно перечислить в `permits` всех прямых наследников; попытка унаследоваться от такого класса вне списка `permits` приводит к ошибке компиляции ([4 Sealed Classes - Java - Oracle Help Center](https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html#:~:text=4%20Sealed%20Classes%20,is%20in%20the%20unnamed)).
- **B.** Все подклассы sealed-класса **обязаны** быть объявлены либо `final`, либо `sealed`, либо `non-sealed` ([Sealed classes in Java 101 - JDriven Blog](https://jdriven.com/blog/2021/10/Sealed-classes#:~:text=Sealed%20classes%20in%20Java%20101,Do%20note)).
- **C.** Подклассы sealed-класса должны находиться либо в том же модуле (при использовании Java Platform Module System), либо в том же пакете, если модуль не задан ([4 Sealed Classes - Java - Oracle Help Center](https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html#:~:text=4%20Sealed%20Classes%20,is%20in%20the%20unnamed)).
- **D.** Sealed-интерфейс не позволяет реализующим классам иметь более одного интерфейса предка.

**Правильные ответы:** A, B, C. (Запечатанные классы и интерфейсы ограничивают наследование: компилятор требует перечня разрешённых наследников и контролирует, что они объявлены в том же модуле или пакете ([4 Sealed Classes - Java - Oracle Help Center](https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html#:~:text=4%20Sealed%20Classes%20,is%20in%20the%20unnamed)). Каждый разрешённый наследник должен далее сам закрыть иерархию (`final`, `sealed` или явно отказаться от запечатывания `non-sealed` ([Sealed classes in Java 101 - JDriven Blog](https://jdriven.com/blog/2021/10/Sealed-classes#:~:text=Sealed%20classes%20in%20Java%20101,Do%20note))). Пункт D неверен – sealed-интерфейс ограничивает лишь реализующие классы/интерфейсы, но они могут иметь и другие интерфейсы.)

## Вопрос 14. Ограничения для record-классов (Java 16+)
Какой из вариантов **не** скомпилируется при объявлении record-класса?

**Варианты ответов:**

- **A.** Объявление record-класса, наследующего обычный класс: `record MyRec() extends ArrayList { }`.
- **B.** Объявление `record` с дополнительными **экземплярными** полями: `record Point(int x, int y) { private int dist; }`.
- **C.** Наличие статических методов и статических полей внутри record-класса.
- **D.** Реализация record-классом интерфейса (implements).

**Правильные ответы:** A, B. (Record – особый финальный класс. Он **не может** явно наследовать другой класс (не считая неявного extends `java.lang.Record`) ([How Java 17 records work | TheServerSide](https://www.theserverside.com/video/How-Java-17-records-work#:~:text=%2F%2F%20This%20will%20NOT%20compile%3A,)). Также record **нельзя** расширить новыми *экземплярными* полями или нестатическими инициализаторами – он хранит только компоненты, объявленные в заголовке ([How Java 17 records work | TheServerSide](https://www.theserverside.com/video/How-Java-17-records-work#:~:text=4,initializers)). Статические поля/методы и реализация интерфейсов допускаются ([How Java 17 records work | TheServerSide](https://www.theserverside.com/video/How-Java-17-records-work#:~:text=7,like%20normal%20classes)) ([How Java 17 records work | TheServerSide](https://www.theserverside.com/video/How-Java-17-records-work#:~:text=Records%20can%20have%20static%20members,interfaces%20similar%20to%20normal%20classes)).)

## Вопрос 15. Pattern Matching `instanceof` – область видимости
Что произойдёт при выполнении следующего кода?

```java
Object obj = "Java";
if (obj instanceof String s) {
    System.out.println(s.toLowerCase());
}
System.out.println(s.toUpperCase());
```

**Варианты ответов:**

- **A.** Код выведет: `java` затем `JAVA`.
- **B.** Код выведет: `java` и выбросит `NullPointerException`.
- **C.** Код не скомпилируется, поскольку переменная `s` недоступна вне блока `if`.
- **D.** Код выведет: `java` затем бросит исключение времени выполнения.

**Правильный ответ:** C. (Переменная, объявленная в условии `instanceof` через паттерн, имеет область видимости **внутри блока `if`**. Попытка обратиться к `s` вне этого блока приводит к ошибке компиляции. Паттерн-матчинг `instanceof` не создает переменную во внешней области видимости.)

## Вопрос 16. Конфликт default-методов интерфейсов
Класс `MyClass` реализует два интерфейса, `IFoo` и `IBar`. В обоих интерфейсах объявлен дефолтный метод `void doIt()`. Что должен сделать разработчик в классе `MyClass`, чтобы разрешить конфликт и скомпилировать класс?

**Варианты ответов:**

- **A.** Переопределить метод `doIt()` в классе `MyClass`, предоставив свою реализацию (или вызвав конкретную реализацию через `IFoo.super.doIt()` или `IBar.super.doIt()`).
- **B.** Ничего – если оба интерфейса имеют одинаковую сигнатуру метода, компилятор автоматически выберет первый по списку интерфейс.
- **C.** Объявить класс `MyClass` абстрактным, оставив разрешение конфликта наследникам.
- **D.** Если один интерфейс унаследован от другого и переопределяет `doIt()`, то проблемы нет: будет унаследована наиболее специфичная реализация.

**Правильные ответы:** A, D. (При конфликте двух default-реализаций компилятор требует явно переопределить метод в классе, иначе ошибка. Поэтому в `MyClass` нужно override `doIt()` и, например, делегировать одной из реализаций через `X.super.doIt()`. Если же один интерфейс уже наследует другой и переопределяет метод, то класс фактически получает одну реализацию – конфликт отсутствует. Автоматического выбора “первого” интерфейса нет – вариант B неверен. Делать класс абстрактным (C) не решит проблему – конфликт останется для наследников.)

## Вопрос 17. Исключение в статическом инициализаторе
Что произойдёт, если в статическом блоке инициализации класса возникло непроверяемое исключение (RuntimeException), которое не перехвачено внутри этого блока?

**Варианты ответов:**

- **A.** Исключение будет проигнорировано JVM, и выполнение программы продолжится – поля класса получат значения по умолчанию.
- **B.** Будет выброшен `ExceptionInInitializerError`, и загрузка класса завершится с ошибкой ([ExceptionInInitializerError (Java Platform SE 7 )](https://docs.oracle.com/javase/7/docs/api/java/lang/ExceptionInInitializerError.html#:~:text=Signals%20that%20an%20unexpected%20exception,initializer%20for%20a%20static%20variable)).
- **C.** Это невозможно – в статических инициализаторах не допускается возникновение неперехваченных исключений.
- **D.** Будет вызван метод `finalize()` для данного класса перед завершением работы.

**Правильный ответ:** B. (Исключение в процессе первичной инициализации класса приводит к ошибке `ExceptionInInitializerError` ([ExceptionInInitializerError (Java Platform SE 7 )](https://docs.oracle.com/javase/7/docs/api/java/lang/ExceptionInInitializerError.html#:~:text=Signals%20that%20an%20unexpected%20exception,initializer%20for%20a%20static%20variable)), обёртывающей возникшее исключение. Этот Error будет брошен при попытке использовать класс, и класс считается не инициализированным.)

## Вопрос 18. Метод `finalize()` объекта
Выберите верные утверждения о методе `Object.finalize()` и механизме финализации в Java:

**Варианты ответов:**

- **A.** Нет гарантии, что метод `finalize()` вообще будет вызван перед удалением объекта сборщиком мусора ([Deprecate Finalization in Java | Baeldung](https://www.baeldung.com/java-18-deprecate-finalization#:~:text=Deprecate%20Finalization%20in%20Java%20,Similarly%2C%20there)).
- **B.** Если объект стал недостижим, финализатор гарантированно выполнится в течение 1 секунды.
- **C.** Объект может «возродиться» в ходе выполнения своего метода `finalize()` – например, сохранить себя в статическом поле, и тогда GC не удалит его сразу ([Why is the finalize() method deprecated in Java 9? - Stack Overflow](https://stackoverflow.com/questions/56139760/why-is-the-finalize-method-deprecated-in-java-9#:~:text=,among%20finalizers)).
- **D.** Начиная с Java 9 метод `finalize()` помечен как deprecated, и в будущем финализация может быть удалена из языка.

**Правильные ответы:** A, C, D. (Финализация ненадежна: нет гарантий времени выполнения или вообще выполнения finalize() ([Why is the finalize() method deprecated in Java 9? - Stack Overflow](https://stackoverflow.com/questions/56139760/why-is-the-finalize-method-deprecated-in-java-9#:~:text=,resurrected)). Объект действительно может снова стать доступным в finalize (хоть JVM вызовет его finalize только один раз) ([Why is the finalize() method deprecated in Java 9? - Stack Overflow](https://stackoverflow.com/questions/56139760/why-is-the-finalize-method-deprecated-in-java-9#:~:text=,among%20finalizers)). Finalize помечен deprecated (Java 9, JEP 421 ([Deprecate Finalization in Java | Baeldung](https://www.baeldung.com/java-18-deprecate-finalization#:~:text=Deprecate%20Finalization%20in%20Java%20,Similarly%2C%20there))) из-за проблем и будет удалён в будущих версиях. Гарантии в п. B ложны.)

## Вопрос 19. Эффективно final и лямбда
Почему следующий код не компилируется?

```java
int counter = 0;
Runnable r = () -> { System.out.println("Count: " + counter); };
counter++;
```

**Варианты ответов:**

- **A.** Лямбда `() -> ...` не может обращаться к внешней переменной `counter`.
- **B.** Переменная `counter` должна быть объявлена как `final`.
- **C.** Изменение переменной `counter` делает её неэффективно финальной (not effectively final), что запрещено для доступа внутри лямбда-выражения.
- **D.** Код скомпилируется, но при выполнении напечатает неправильное значение счётчика.

**Правильный ответ:** C. (Лямбда может использовать внешние локальные переменные, только если они **эффективно final** – не изменяются после инициализации. В данном случае после создания лямбды переменная `counter` изменяется (`counter++`), поэтому компиляция завершается ошибкой. Необязательно явно объявлять `final`, достаточно не изменять переменную.)

## Вопрос 20. Конкатенация строк и числа
Какой будет выход программы?

```java
int x = 1, y = 2;
System.out.print("" + x + y + " ");
System.out.print(x + y + " ");
System.out.print(x + " " + y);
```

**Варианты ответов:**

- **A.** `12 3 1 2`
- **B.** `3 3 12`
- **C.** `12 12 3`
- **D.** `3 12 12`

**Правильный ответ:** A. (В первом вызове `"" + x + y` сначала пустая строка превращает всё последующее в строку, поэтому получается конкатенация `"1"` + `"2"` = `"12"` ([OCP Java 17 Developer 1Z0-829 Free Practice Questions - MyExamCloud Blog Article](https://www.myexamcloud.com/blog/ocp-java-17-developer-1z0-829-free-practice-questions.article#:~:text=Which%20is%20the%20output%3F)). Во втором вызове `x + y + " "` сначала складываются числа (1+2=3), затем конкатенация даёт `"3 "`. В третьем – `"1 "` + `"2"` выводится как `1 2`. Итого: `12 3 1 2`.) 

