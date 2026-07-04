Реализовать класс планировщика задач, который запускает задачи по расписанию.

```java
public interface CallbackScheduler extends AutoCloseable {  
    void schedule(Runnable callback, Instant when);  
}

public class CallbackSchedulerImpl implements CallbackScheduler {  
  
    public CallbackSchedulerImpl() {  
    }  
  
    @Override  
    public void schedule(Runnable callback, Instant when) {   
    }  
  
    @Override  
    public void close() throws Exception {  
    }  
}

public class CallbackSchedulerTest {  
  
    private CallbackScheduler scheduler;  
  
    private static class CallbackResult {  
        boolean isDone = false;  
    }  
  
    @BeforeEach  
    public void setUp() {  
        scheduler = new CallbackSchedulerImpl();  
    }  
  
    @AfterEach  
    public void tearDown() throws Exception {  
        scheduler.close();  
    }  
  
    @Test  
    public void testSimple() throws InterruptedException {  
  
        final CallbackResult result = new CallbackResult();  
  
        scheduler.schedule(  
            () -> {  
                synchronized (result) {  
                    result.isDone = true;  
                    result.notify();  
                }  
            },  
            Instant.now().plusSeconds(2)  
        );  
  
        synchronized (result) {  
            result.wait();  
            assertTrue(result.isDone);  
        }  
    }  
}
```

---

```kotlin
// Нужно было написать реализацию паттерна pub/sub на основании этого интерфейса

interface EventBus : AutoCloseable {
    fun subscribe(handler: (OrderCreated) -> Unit): SubscriptionToken
    fun unsubscribe(token: SubscriptionToken)
    fun publish(event: OrderCreated)
    fun close()
}

@Test
fun testEventDeliveryWithToken() {
    val eventBus = EventBusImpl()

    val orderCount = AtomicInteger()
    val lastOrderId = AtomicReference<String>("")

    val token = eventBus.subscribe { order ->
        orderCount.incrementAndGet()
        lastOrderId.set(order.orderId)
    }

    eventBus.publish(OrderCreated("ORDER-123"))
    Thread.sleep(100)

    assertEquals(1, orderCount.get())
    assertEquals("ORDER-123", lastOrderId.get())

    eventBus.close()
}
```

---

Есть таблица сотрудников, которые знают языки программирования.
Нужно написать запрос, который выберет имена сотрудников, которые знают SQL и ещё хотя бы один любой язык.

| name | lang |
|------|------|
| Nick | C#   |
| Nick | SQL  |
| Eva  | Rust |
| Vika | SQL  |
| Ivan | SQL  |
| Ivan | SQL  |

---

Core review Spring Boot проекта

```java
@Component
public class InterviewCandidateListener {

    private final InterviewService interviewService;

    public InterviewCandidateListener(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @KafkaListener(
        topics = "interview-candidates",
        groupId = "interview-score-service"
    )
    public void listen(Candidate candidate) {
        interviewService.process(candidate);
    }
}

@Component
public class InterviewService {

    private final ScoreRepository scoreRepository;
    private final TransactionTemplate transactionTemplate;
    private final InterviewScoreMLService interviewScoreMLService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InterviewService(ScoreRepository scoreRepository,
        TransactionTemplate transactionTemplate,
        InterviewScoreMLService interviewScoreMLService) {

        this.scoreRepository = scoreRepository;
        this.transactionTemplate = transactionTemplate;
        this.interviewScoreMLService = interviewScoreMLService;
    }

    /**
     * Метод считает сколько очков заработал кандидат,
     * сохраняет результат в базу и кидает callback об этом во внешний сервис
     */
    public void process(Candidate c) {

        transactionTemplate.executeWithoutResult(status -> {
            Score s = interviewScoreMLService.compute(c);
            String body = objectMapper.writeValueAsString(Map.of(c.getName(), s));
            Mono < ResponseEntity < Void >> request = webClient.create()
              .post()
              .body(BodyInserters.fromValue(body))
              .retrieve()
              .toBodilessEntity();

            scoreRepository.saveScore(s);
        });

    }
}

class Candidate {
    private final String name;
    private final List < Integer > tasksSolvedId;

    public Candidate(String name, List < Integer > tasksSolvedId) {
        this.name = name;
        this.tasksSolvedId = tasksSolvedId;
    }

    public String getName() {
        return name;
    }

    public List < Integer > getTasksSolvedId() {
        return tasksSolvedId;
    }
}

class Score {
    private final String name;
    private final int score;

    public Score(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }
}
```

---

Задача

```java
/**
 * Интерфейс для взаимодействия с аппаратной частью банкомата.
 */
interface Hardware {

    /**
     * Возвращает массив с количеством купюр по номиналам 50, 100, 500, 1000, 5000.
     * Метод работает медленно.
     *
     * @return массив, где каждый элемент соответствует количеству купюр определенного номинала.
     *         Например, [10, 20, 30, 40, 50] означает:
     *             - 10 купюр номиналом 50 рублей
     *             - 20 купюр номиналом 100 рублей
     *             - 30 купюр номиналом 500 рублей
     *             - 40 купюр номиналом 1000 рублей
     *             - 50 купюр номиналом 5000 рублей
     */
    int[] getBillsCounts();
    
    /**
     * Загружает в бокс выдачи указанные купюры.
     *
     * @param billsCounts массив с количеством купюр по номиналам [50, 100, 500, 1000, 5000].
     *                    Например, [0, 1, 0, 2, 0] означает: ->2100
     *                        - 0 купюр номиналом 50 рублей
     *                        - 1 купюру номиналом 100 рублей
     *                        - 0 купюр номиналом 500 рублей
     *                        - 2 купюры номиналом 1000 рублей
     *                        - 0 купюр номиналом 5000 рублей
     */
    void giveBills(int[] billsCounts);
 }

/*
* Тут нужно писать код
* Реализовать метод, который принимает запрашиваюмую сумму и выдает её, через вызов giveBills(). НЕ нужно писать реализацию методов интерфейса Hardware!
*/
class MyATM {

    /**
     * Аппаратная часть банкомата.
     */
    Hardware hardware;

}   
```