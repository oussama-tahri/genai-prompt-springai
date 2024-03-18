package com.tahrioussama.genaipromptspringai.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OpenAiRestController {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    // this ChatClient is generic but if we want to use a specific chat client we can,
    // for example : OpenAiChatClient
    @Autowired
    private ChatClient chatClient;


    // we can use this method to generate a response by passing the input in parameter
    // example : http://localhost:8080/chat?message=who is Oussama Tahri
    @GetMapping("/chat")
    public String chat(String message) {
        return chatClient.call(message);
    }

    // in this method we are using a template to generate an input/output, we only need to pass parameters
    // example : http://localhost:8080/movies?category=action&year=2018
    @GetMapping("/movies")
    public Map movies(@RequestParam(name = "category", defaultValue = "action") String category,
                               @RequestParam(name = "year", defaultValue = "2022") int year) throws JsonProcessingException {
        // OpenAiApi should take the api key generated from "https://platform.openai.com/api-keys"
        OpenAiApi openAiApi = new OpenAiApi(apiKey);

        // we need to specify some options in order to build the chat
        // we need to pass the model (gpt-3.5-turbo or gpt-4 ...)
        // the temperature is between 0 and 1, if the temperature equals 1 then for each input passed should return different output
        // but if the temperature is equal to 0 then for every input will return the same output
        // Tokens are used for the input, the model convert words to tokens, On output, they convert tokens back to words.
        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions
                .builder()
                .withModel("gpt-3.5-turbo")
                .withTemperature(0.5F)
                .withMaxTokens(2000)
                .build();
        OpenAiChatClient openAiChatClient = new OpenAiChatClient(openAiApi,openAiChatOptions);

        // The prompt contains the input sent by the user
        // In this template we generate an input and output based on two parameters
        // the category and the year given by the user
        // the output should contain all the information given in the template
        // the user should only give the category & year
        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(
                """
                        I need you to give me the best movie on the given category : {category}
                        on the given year : {year}.
                        The output should be in JSON format including the following fields :
                        - category<The given category>
                        - year<The given year>
                        - title<The title of the movie>
                        - producer<The producer of the movie>
                        - actors<A list of main actors of the movie>
                        - summary<A very small summary of the movie>
                        """
        );
        // in order to create the prompt template we need to pass the parameters needed
        Prompt prompt = promptTemplate.create(Map.of("category",category,"year", year));
        // the response should be based on the prompt, it reads the prompt and gives response
        ChatResponse response = openAiChatClient.call(prompt);

        // to return the result in Json format we need to call getResult, getOutput, getContent methods
        String content = response.getResult().getOutput().getContent();
        // and return a new object mapper that reads the content and convert it to Map
        return new ObjectMapper().readValue(content, Map.class);
    }
}