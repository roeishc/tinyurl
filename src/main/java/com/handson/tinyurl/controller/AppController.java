package com.handson.tinyurl.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.tinyurl.model.NewTinyRequest;
import com.handson.tinyurl.model.User;
import com.handson.tinyurl.model.UserClickOut;
import com.handson.tinyurl.repository.UserClickRepository;
import com.handson.tinyurl.repository.UserRepository;
import com.handson.tinyurl.service.Redis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.handson.tinyurl.model.User.UserBuilder.anUser;
import static com.handson.tinyurl.model.UserClick.UserClickBuilder.anUserClick;
import static com.handson.tinyurl.model.UserClickKey.UserClickKeyBuilder.anUserClickKey;
import static com.handson.tinyurl.util.Dates.getCurMonth;
import static org.springframework.data.util.StreamUtils.createStreamFromIterator;

@RestController
public class AppController {

    private static final int MAX_RETRIES = 4;

    private static final int TINY_LENGTH = 6;

    @Value("${base.url}")
    private String baseUrl;

    private final Random random = new Random();

    @Autowired
    private Redis redis;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserClickRepository userClickRepository;

    @RequestMapping(value = "getKey", method = RequestMethod.GET)
    public String getKey(@RequestParam String key){
        return redis.get(key).toString();
    }

    @RequestMapping(value = "setKey", method = RequestMethod.GET)
    public Boolean setKey(@RequestParam String key, @RequestParam String value){
        return redis.set(key, value);
    }

    @RequestMapping(value = "/tiny", method = RequestMethod.POST)
    public String generate(@RequestBody NewTinyRequest request) throws JsonProcessingException {
        String tinyCode = generateTinyCode();
        int i = 0;
        request.setLongUrl(request.getLongUrl().startsWith("http://") ?
                request.getLongUrl().substring(7) : request.getLongUrl());
        while (!redis.set(tinyCode, om.writeValueAsString(request)) && i < MAX_RETRIES) {
            tinyCode = generateTinyCode();
            i++;
        }
        if (i == MAX_RETRIES) throw new RuntimeException("SPACE IS FULL");
        return baseUrl + tinyCode + "/";
    }

    @RequestMapping(value = {"/{tiny}", "/{tiny}/"}, method = RequestMethod.GET)
    public ModelAndView getTiny(@PathVariable String tiny) throws JsonProcessingException {
        if (tiny.startsWith("swagger-ui.html"))
            return new ModelAndView("redirect:" + baseUrl + tiny);
        Object tinyRequestStr = redis.get(tiny);
        NewTinyRequest tinyRequest = om.readValue(tinyRequestStr.toString(), NewTinyRequest.class);
        if (tinyRequest.getLongUrl() != null) {
            String userName = tinyRequest.getUserName();
            if (userName != null) {
                incrementMongoField(userName, "allUrlClicks", 1);
                incrementMongoField(userName, "shorts."  + tiny + ".clicks." + getCurMonth(), 1);
                userClickRepository.save(anUserClick()
                        .userClickKey(anUserClickKey()
                                .withUserName(userName)
                                .withClickTime(new Date())
                                .build())
                        .tiny(tiny)
                        .longUrl(tinyRequest.getLongUrl())
                        .build());
            }
            String longUrl = tinyRequest.getLongUrl();
            return new ModelAndView("redirect:" + (longUrl.startsWith("https://") ?
                    longUrl : "https://" + longUrl));
        } else {
            throw new RuntimeException(tiny + " not found");
        }
    }

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public User createUser(@RequestParam String name) {
        User user = anUser().withName(name).build();
        user = userRepository.insert(user);
        return user;
    }

    @RequestMapping(value = "/user/{name}", method = RequestMethod.GET)
    public User getUser(@RequestParam String name) {
        return userRepository.findFirstByName(name);
    }

    @RequestMapping(value = "/user/{name}/clicks", method = RequestMethod.GET)
    public List<UserClickOut> getUserClicks(@RequestParam String name) {
        List<UserClickOut> userClicks = createStreamFromIterator( userClickRepository.findByUserName(name).iterator())
                .map(userClick -> UserClickOut.of(userClick))
                .collect(Collectors.toList());
        return userClicks;
    }

    private String generateTinyCode() {
        String charPool = "ABCDEFHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < TINY_LENGTH; i++)
            res.append(charPool.charAt(random.nextInt(charPool.length())));
        return res.toString();
    }

    private void incrementMongoField(String userName, String key, int inc){
        Query query = Query.query(Criteria.where("name").is(userName));
        Update update = new Update().inc(key, inc);
        mongoTemplate.updateFirst(query, update, "users");
    }

}
