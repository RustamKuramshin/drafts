package ru.kuramshindev.spring.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import ru.kuramshindev.spring.book.api.CreateBookRequest;
import ru.kuramshindev.spring.book.client.BookCatalogClient;
import ru.kuramshindev.spring.book.client.BookMetadataResponse;
import ru.kuramshindev.spring.book.persistence.BookRepository;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @MockitoBean
    private BookCatalogClient bookCatalogClient;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    @Test
    void createBook_persistsEntityAndReturnsEnrichedPayload() throws Exception {
        CreateBookRequest request = new CreateBookRequest(
                "Spring in Action",
                "Craig Walls",
                "9781617297571"
        );

        given(bookCatalogClient.fetchMetadata(request.isbn()))
                .willReturn(new BookMetadataResponse(
                        "open-library",
                        "Popular Spring Boot guide used as interview practice material"
                ));

        mockMvc.perform(post("/api/books")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Spring in Action"))
                .andExpect(jsonPath("$.author").value("Craig Walls"))
                .andExpect(jsonPath("$.isbn").value("9781617297571"))
                .andExpect(jsonPath("$.sourceSystem").value("open-library"))
                .andExpect(jsonPath("$.summary").value("Popular Spring Boot guide used as interview practice material"));

        Book savedBook = bookRepository.findAll().getFirst();
        assertThat(savedBook.getTitle()).isEqualTo("Spring in Action");
        assertThat(savedBook.getSourceSystem()).isEqualTo("open-library");
        assertThat(savedBook.getSummary()).isEqualTo("Popular Spring Boot guide used as interview practice material");
        verify(bookCatalogClient).fetchMetadata("9781617297571");
    }

    @Test
    void getBooks_returnsDataFromH2Database() throws Exception {
        bookRepository.save(new Book(
                "Effective Java",
                "Joshua Bloch",
                "9780134685991",
                "seed-data",
                "Best practices for production Java applications"
        ));
        bookRepository.save(new Book(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                "seed-data",
                "Collection of practical refactoring heuristics"
        ));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Effective Java"))
                .andExpect(jsonPath("$[0].sourceSystem").value("seed-data"))
                .andExpect(jsonPath("$[1].title").value("Clean Code"));
    }

    @Test
    void getBook_returnsSinglePersistedEntity() throws Exception {
        Book savedBook = bookRepository.save(new Book(
                "Java Concurrency in Practice",
                "Brian Goetz",
                "9780321349606",
                "seed-data",
                "Concurrency primitives, patterns, and tradeoffs"
        ));

        mockMvc.perform(get("/api/books/{id}", savedBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedBook.getId()))
                .andExpect(jsonPath("$.title").value("Java Concurrency in Practice"))
                .andExpect(jsonPath("$.isbn").value("9780321349606"));
    }
}
