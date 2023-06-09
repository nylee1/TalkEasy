package com.talkeasy.server.service.aac;

import com.talkeasy.server.common.PagedResponse;
import com.talkeasy.server.common.exception.ArgumentMismatchException;
import com.talkeasy.server.common.exception.ResourceNotFoundException;
import com.talkeasy.server.domain.aac.AAC;
import com.talkeasy.server.domain.aac.AACCategory;
import com.talkeasy.server.domain.aac.CustomAAC;
import com.talkeasy.server.domain.member.Member;
import com.talkeasy.server.dto.aac.CustomAACDto;
import com.talkeasy.server.dto.aac.OpenAIMessage;
import com.talkeasy.server.dto.aac.ResponseAACDto;
import com.talkeasy.server.dto.aac.ResponseAACListDto;
import com.talkeasy.server.dto.chat.ChatTextDto;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class AACService {

    private final MongoTemplate mongoTemplate;

    @Value("${openAI.api.key}")
    private String apiKey;

    // 카테고리 종류
    public PagedResponse<AACCategory> getCategory() {

        List<AACCategory> result = mongoTemplate.findAll(AACCategory.class, "aac_category");

        return new PagedResponse(HttpStatus.OK, result, 1);

    }

    // 카테고리별 aac 조회
    public PagedResponse<ResponseAACListDto> getAacByCategory(String categoryId) {

        // 카테고리별 고정 내용 출력
        List<ResponseAACDto> fixedResult = mongoTemplate.find(Query.query(Criteria.where("category").is(categoryId)
                .and("fixed").is(1)).with(Sort.by(Sort.Direction.ASC, "title")), AAC.class)
                .stream()
                .map(ResponseAACDto::new)
                .collect(Collectors.toList());

        // 카테고리별 일반 내용 출력
        Query query = new Query(Criteria.where("category").is(categoryId).and("fixed").is(0)).with(Sort.by(Sort.Direction.ASC, "title"));
        List<ResponseAACDto> aacList = mongoTemplate.find(query, AAC.class).stream().map((a) -> new ResponseAACDto((a))).collect(Collectors.toList());

        // 고정/일반 내용 한번에 출력
        ResponseAACListDto categoryList = ResponseAACListDto.builder().fixedList(fixedResult).aacList(aacList).build();

        return new PagedResponse(HttpStatus.OK, categoryList, 1);

    }

    public PagedResponse<ResponseAACListDto> getAacByCustom(String userId) {

        Query query = new Query(Criteria.where("userId").is(userId)).with(Sort.by(Sort.Direction.ASC, "title"));
        List<ResponseAACDto> aacList = mongoTemplate.find(query, CustomAAC.class)
                .stream().map((a) -> new ResponseAACDto(a)).collect(Collectors.toList());

        ResponseAACListDto categoryList = ResponseAACListDto.builder().fixedList(new ArrayList<>()).aacList(aacList).build();

        return new PagedResponse(HttpStatus.OK, categoryList, 1);

    }

    // aac 연관 동사 조회
    public PagedResponse<ResponseAACDto> getRelativeVerb(String aacId) {

        Query query = new Query(Criteria.where("id").is(aacId));
        AAC aac = mongoTemplate.findOne(query, AAC.class);

        List<ResponseAACDto> result = Arrays.stream(aac.getRelative_verb().split(" "))
                .filter(a -> !a.equals("0"))
                .map(a -> mongoTemplate.findOne(new Query(Criteria.where("id").is(a)), AAC.class))
                .map(a -> new ResponseAACDto(a))
                .collect(Collectors.toList());

        Collections.sort(result, Comparator.comparing(ResponseAACDto::getTitle));

        return new PagedResponse(HttpStatus.OK, result, 1);

    }


    /* 커스텀 AAC */
    public String deleteCustomAac(String aacId, String userId) {

        Query query = new Query(Criteria.where("id").is(aacId));
        CustomAAC customAAC = mongoTemplate.findOne(query, CustomAAC.class);

        isMine(userId, customAAC.getUserId());

        mongoTemplate.remove(customAAC);
        return aacId;
    }

    public String postCustomAac(CustomAACDto customAac, String userId) {

        Optional.ofNullable(mongoTemplate.findOne(Query.query(Criteria.where("id").is(userId)), Member.class)).orElseThrow(
                () -> new ResourceNotFoundException("Member", "userId", userId)
        );

        CustomAAC customAAC = mongoTemplate.insert(CustomAAC.builder().userId(userId).text(customAac.getTitle()).build());
        return customAAC.getId();

    }

    public String putCustomAac(String aacId, CustomAACDto customAac, String userId) {

        log.info("{}", aacId);
        Query query = new Query(Criteria.where("id").is(aacId));
        CustomAAC customAAC = mongoTemplate.findOne(query, CustomAAC.class);

        isMine(userId, customAAC.getUserId());

        customAAC.setText(customAac.getTitle());
        mongoTemplate.save(customAAC);

        return customAAC.getId();
    }

    public void isMine(String userId, String aacUserId) {

        if (!userId.equals(aacUserId)) {
            throw new ArgumentMismatchException("본인이 작성한 aac가 아닙니다");
        }

    }

    /* gpt */
    public String getGenereteText(ChatTextDto text) {

        String[] input_texts = text.getText().split(",");
        if (input_texts.length == 1){
            return input_texts[0];
        }

        String data = text.getText().replace(",", "");

        /**/
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        List<OpenAIMessage> messages = new ArrayList<>();

        messages.add(new OpenAIMessage("user", data + " .' 한글로 완전한 문장으로 완성해줘."));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messages", messages);
        requestBody.put("model","gpt-3.5-turbo");
        requestBody.put("temperature", 0.0f);
        requestBody.put("max_tokens", 50);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity("https://api.openai.com/v1/chat/completions", requestEntity, Map.class);
        Map<String, Object> responseBody = response.getBody();

        // choices 필드의 값을 추출
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        // 첫 번째 choice의 message 필드의 값을 추출
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        // message 필드의 content 값을 추출
        String content = (String) message.get("content");

        return content.replace("\"", "");
    }

}
