package com.example.orderservice.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerValidationTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private OrderService orderService;

  @Test
  void createOrderRejectsInvalidRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "productName": "",
                                  "quantity": 0,
                                  "amount": 0,
                                  "customerEmail": "not-an-email"
                                }
                                """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(orderService);
  }
}
