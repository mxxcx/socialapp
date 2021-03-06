package com.ninja.socialapp.web.rest;

import com.ninja.socialapp.SocialappApp;

import com.ninja.socialapp.domain.TwitterAccount;
import com.ninja.socialapp.domain.Avatar;
import com.ninja.socialapp.domain.Header;
import com.ninja.socialapp.domain.Proxy;
import com.ninja.socialapp.domain.TwitterMessage;
import com.ninja.socialapp.repository.TwitterAccountRepository;
import com.ninja.socialapp.service.TwitterAccountService;
import com.ninja.socialapp.repository.search.TwitterAccountSearchRepository;
import com.ninja.socialapp.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ninja.socialapp.domain.enumeration.TwitterStatus;
import com.ninja.socialapp.domain.enumeration.TwitterStatus;
/**
 * Test class for the TwitterAccountResource REST controller.
 *
 * @see TwitterAccountResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SocialappApp.class)
public class TwitterAccountResourceIntTest {

    private static final String DEFAULT_EMAIL = "j3@[211.57.255.198]";
    private static final String UPDATED_EMAIL = "\"\"@[06.204.11.240]";

    private static final String DEFAULT_CONSUMER_KEY = "AAAAAAAAAA";
    private static final String UPDATED_CONSUMER_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_CONSUMER_SECRET = "AAAAAAAAAA";
    private static final String UPDATED_CONSUMER_SECRET = "BBBBBBBBBB";

    private static final String DEFAULT_ACCESS_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_ACCESS_TOKEN = "BBBBBBBBBB";

    private static final String DEFAULT_ACCESS_TOKEN_SECRET = "AAAAAAAAAA";
    private static final String UPDATED_ACCESS_TOKEN_SECRET = "BBBBBBBBBB";

    private static final String DEFAULT_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_USERNAME = "BBBBBBBBBB";

    private static final String DEFAULT_PHONE = "212434543";
    private static final String UPDATED_PHONE = "272342323";

    private static final TwitterStatus DEFAULT_STATUS = TwitterStatus.PENDING_UPDATE;
    private static final TwitterStatus UPDATED_STATUS = TwitterStatus.PENDING_UPDATE;

    private static final TwitterStatus DEFAULT_PREV_STATUS = TwitterStatus.PENDING_UPDATE;
    private static final TwitterStatus UPDATED_PREV_STATUS = TwitterStatus.PENDING_UPDATE;

    @Autowired
    private TwitterAccountRepository twitterAccountRepository;

    @Autowired
    private TwitterAccountService twitterAccountService;

    @Autowired
    private TwitterAccountSearchRepository twitterAccountSearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restTwitterAccountMockMvc;

    private TwitterAccount twitterAccount;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TwitterAccountResource twitterAccountResource = new TwitterAccountResource(twitterAccountService);
        this.restTwitterAccountMockMvc = MockMvcBuilders.standaloneSetup(twitterAccountResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TwitterAccount createEntity(EntityManager em) {
        TwitterAccount twitterAccount = new TwitterAccount()
            .email(DEFAULT_EMAIL)
            .consumerKey(DEFAULT_CONSUMER_KEY)
            .consumerSecret(DEFAULT_CONSUMER_SECRET)
            .accessToken(DEFAULT_ACCESS_TOKEN)
            .accessTokenSecret(DEFAULT_ACCESS_TOKEN_SECRET)
            .username(DEFAULT_USERNAME)
            .phone(DEFAULT_PHONE)
            .status(DEFAULT_STATUS)
            .prevStatus(DEFAULT_PREV_STATUS);
        // Add required entity
        Avatar avatar = AvatarResourceIntTest.createEntity(em);
        em.persist(avatar);
        em.flush();
        twitterAccount.setAvatar(avatar);
        // Add required entity
        Header header = HeaderResourceIntTest.createEntity(em);
        em.persist(header);
        em.flush();
        twitterAccount.setHeader(header);
        // Add required entity
        Proxy proxy = ProxyResourceIntTest.createEntity(em);
        em.persist(proxy);
        em.flush();
        twitterAccount.setProxy(proxy);
        // Add required entity
        TwitterMessage message = TwitterMessageResourceIntTest.createEntity(em);
        em.persist(message);
        em.flush();
        twitterAccount.setMessage(message);
        return twitterAccount;
    }

    @Before
    public void initTest() {
        twitterAccountSearchRepository.deleteAll();
        twitterAccount = createEntity(em);
    }

    @Test
    @Transactional
    public void createTwitterAccount() throws Exception {
        int databaseSizeBeforeCreate = twitterAccountRepository.findAll().size();

        // Create the TwitterAccount
        restTwitterAccountMockMvc.perform(post("/api/twitter-accounts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(twitterAccount)))
            .andExpect(status().isCreated());

        // Validate the TwitterAccount in the database
        List<TwitterAccount> twitterAccountList = twitterAccountRepository.findAll();
        assertThat(twitterAccountList).hasSize(databaseSizeBeforeCreate + 1);
        TwitterAccount testTwitterAccount = twitterAccountList.get(twitterAccountList.size() - 1);
        assertThat(testTwitterAccount.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testTwitterAccount.getConsumerKey()).isEqualTo(DEFAULT_CONSUMER_KEY);
        assertThat(testTwitterAccount.getConsumerSecret()).isEqualTo(DEFAULT_CONSUMER_SECRET);
        assertThat(testTwitterAccount.getAccessToken()).isEqualTo(DEFAULT_ACCESS_TOKEN);
        assertThat(testTwitterAccount.getAccessTokenSecret()).isEqualTo(DEFAULT_ACCESS_TOKEN_SECRET);
        assertThat(testTwitterAccount.getUsername()).isEqualTo(DEFAULT_USERNAME);
        assertThat(testTwitterAccount.getPhone()).isEqualTo(DEFAULT_PHONE);
        assertThat(testTwitterAccount.getStatus()).isEqualTo(DEFAULT_STATUS);
        assertThat(testTwitterAccount.getPrevStatus()).isEqualTo(DEFAULT_PREV_STATUS);

        // Validate the TwitterAccount in Elasticsearch
        TwitterAccount twitterAccountEs = twitterAccountSearchRepository.findOne(testTwitterAccount.getId());
        assertThat(twitterAccountEs).isEqualToComparingFieldByField(testTwitterAccount);
    }

    @Test
    @Transactional
    public void createTwitterAccountWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = twitterAccountRepository.findAll().size();

        // Create the TwitterAccount with an existing ID
        twitterAccount.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restTwitterAccountMockMvc.perform(post("/api/twitter-accounts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(twitterAccount)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<TwitterAccount> twitterAccountList = twitterAccountRepository.findAll();
        assertThat(twitterAccountList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkEmailIsRequired() throws Exception {
        int databaseSizeBeforeTest = twitterAccountRepository.findAll().size();
        // set the field null
        twitterAccount.setEmail(null);

        // Create the TwitterAccount, which fails.

        restTwitterAccountMockMvc.perform(post("/api/twitter-accounts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(twitterAccount)))
            .andExpect(status().isBadRequest());

        List<TwitterAccount> twitterAccountList = twitterAccountRepository.findAll();
        assertThat(twitterAccountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkConsumerKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = twitterAccountRepository.findAll().size();
        // set the field null
        twitterAccount.setConsumerKey(null);

        // Create the TwitterAccount, which fails.

        restTwitterAccountMockMvc.perform(post("/api/twitter-accounts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(twitterAccount)))
            .andExpect(status().isBadRequest());

        List<TwitterAccount> twitterAccountList = twitterAccountRepository.findAll();
        assertThat(twitterAccountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkConsumerSecretIsRequired() throws Exception {
        int databaseSizeBeforeTest = twitterAccountRepository.findAll().size();
        // set the field null
        twitterAccount.setConsumerSecret(null);

        // Create the TwitterAccount, which fails.

        restTwitterAccountMockMvc.perform(post("/api/twitter-accounts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(twitterAccount)))
            .andExpect(status().isBadRequest());

        List<TwitterAccount> twitterAccountList = twitterAccountRepository.findAll();
        assertThat(twitterAccountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkAccessTokenIsRequired() throws Exception {
        int databaseSizeBeforeTest = twitterAccountRepository.findAll().size();
        // set the field null
        twitterAccount.setAccessToken(null);

        // Create the TwitterAccount, which fails.

        restTwitterAccountMockMvc.perform(post("/api/twitter-accounts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(twitterAccount)))
            .andExpect(status().isBadRequest());

        List<TwitterAccount> twitterAccountList = twitterAccountRepository.findAll();
        assertThat(twitterAccountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkAccessTokenSecretIsRequired() throws Exception {
        int databaseSizeBeforeTest = twitterAccountRepository.findAll().size();
        // set the field null
        twitterAccount.setAccessTokenSecret(null);

        // Create the TwitterAccount, which fails.

        restTwitterAccountMockMvc.perform(post("/api/twitter-accounts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(twitterAccount)))
            .andExpect(status().isBadRequest());

        List<TwitterAccount> twitterAccountList = twitterAccountRepository.findAll();
        assertThat(twitterAccountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllTwitterAccounts() throws Exception {
        // Initialize the database
        twitterAccountRepository.saveAndFlush(twitterAccount);

        // Get all the twitterAccountList
        restTwitterAccountMockMvc.perform(get("/api/twitter-accounts?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(twitterAccount.getId().intValue())))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL.toString())))
            .andExpect(jsonPath("$.[*].consumerKey").value(hasItem("")))
            .andExpect(jsonPath("$.[*].consumerSecret").value(hasItem("")))
            .andExpect(jsonPath("$.[*].accessToken").value(hasItem("")))
            .andExpect(jsonPath("$.[*].accessTokenSecret").value(hasItem("")))
            .andExpect(jsonPath("$.[*].username").value(hasItem(DEFAULT_USERNAME.toString())))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].prevStatus").value(hasItem(DEFAULT_PREV_STATUS.toString())));
    }

    @Test
    @Transactional
    public void getTwitterAccount() throws Exception {
        // Initialize the database
        twitterAccountRepository.saveAndFlush(twitterAccount);

        // Get the twitterAccount
        restTwitterAccountMockMvc.perform(get("/api/twitter-accounts/{id}", twitterAccount.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(twitterAccount.getId().intValue()))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL.toString()))
            .andExpect(jsonPath("$.consumerKey").value(DEFAULT_CONSUMER_KEY.toString()))
            .andExpect(jsonPath("$.consumerSecret").value(DEFAULT_CONSUMER_SECRET.toString()))
            .andExpect(jsonPath("$.accessToken").value(DEFAULT_ACCESS_TOKEN.toString()))
            .andExpect(jsonPath("$.accessTokenSecret").value(DEFAULT_ACCESS_TOKEN_SECRET.toString()))
            .andExpect(jsonPath("$.username").value(DEFAULT_USERNAME.toString()))
            .andExpect(jsonPath("$.phone").value(DEFAULT_PHONE.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.prevStatus").value(DEFAULT_PREV_STATUS.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingTwitterAccount() throws Exception {
        // Get the twitterAccount
        restTwitterAccountMockMvc.perform(get("/api/twitter-accounts/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateTwitterAccount() throws Exception {
        // Initialize the database
        twitterAccountService.save(twitterAccount);

        int databaseSizeBeforeUpdate = twitterAccountRepository.findAll().size();

        // Update the twitterAccount
        TwitterAccount updatedTwitterAccount = twitterAccountRepository.findOne(twitterAccount.getId());
        updatedTwitterAccount
            .email(UPDATED_EMAIL)
            .consumerKey(UPDATED_CONSUMER_KEY)
            .consumerSecret(UPDATED_CONSUMER_SECRET)
            .accessToken(UPDATED_ACCESS_TOKEN)
            .accessTokenSecret(UPDATED_ACCESS_TOKEN_SECRET)
            .username(UPDATED_USERNAME)
            .phone(UPDATED_PHONE)
            .status(UPDATED_STATUS)
            .prevStatus(UPDATED_PREV_STATUS);

        restTwitterAccountMockMvc.perform(put("/api/twitter-accounts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedTwitterAccount)))
            .andExpect(status().isOk());

        // Validate the TwitterAccount in the database
        List<TwitterAccount> twitterAccountList = twitterAccountRepository.findAll();
        assertThat(twitterAccountList).hasSize(databaseSizeBeforeUpdate);
        TwitterAccount testTwitterAccount = twitterAccountList.get(twitterAccountList.size() - 1);
        assertThat(testTwitterAccount.getEmail()).isEqualTo(UPDATED_EMAIL);
        assertThat(testTwitterAccount.getConsumerKey()).isEqualTo(UPDATED_CONSUMER_KEY);
        assertThat(testTwitterAccount.getConsumerSecret()).isEqualTo(UPDATED_CONSUMER_SECRET);
        assertThat(testTwitterAccount.getAccessToken()).isEqualTo(UPDATED_ACCESS_TOKEN);
        assertThat(testTwitterAccount.getAccessTokenSecret()).isEqualTo(UPDATED_ACCESS_TOKEN_SECRET);
        assertThat(testTwitterAccount.getUsername()).isEqualTo(UPDATED_USERNAME);
        assertThat(testTwitterAccount.getPhone()).isEqualTo(UPDATED_PHONE);
        assertThat(testTwitterAccount.getStatus()).isEqualTo(UPDATED_STATUS);
        assertThat(testTwitterAccount.getPrevStatus()).isEqualTo(UPDATED_PREV_STATUS);

        // Validate the TwitterAccount in Elasticsearch
        TwitterAccount twitterAccountEs = twitterAccountSearchRepository.findOne(testTwitterAccount.getId());
        assertThat(twitterAccountEs).isEqualToComparingFieldByField(testTwitterAccount);
    }

    @Test
    @Transactional
    public void updateNonExistingTwitterAccount() throws Exception {
        int databaseSizeBeforeUpdate = twitterAccountRepository.findAll().size();

        // Create the TwitterAccount

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restTwitterAccountMockMvc.perform(put("/api/twitter-accounts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(twitterAccount)))
            .andExpect(status().isCreated());

        // Validate the TwitterAccount in the database
        List<TwitterAccount> twitterAccountList = twitterAccountRepository.findAll();
        assertThat(twitterAccountList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteTwitterAccount() throws Exception {
        // Initialize the database
        twitterAccountService.save(twitterAccount);

        int databaseSizeBeforeDelete = twitterAccountRepository.findAll().size();

        // Get the twitterAccount
        restTwitterAccountMockMvc.perform(delete("/api/twitter-accounts/{id}", twitterAccount.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate Elasticsearch is empty
        boolean twitterAccountExistsInEs = twitterAccountSearchRepository.exists(twitterAccount.getId());
        assertThat(twitterAccountExistsInEs).isFalse();

        // Validate the database is empty
        List<TwitterAccount> twitterAccountList = twitterAccountRepository.findAll();
        assertThat(twitterAccountList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchTwitterAccount() throws Exception {
        // Initialize the database
        twitterAccountService.save(twitterAccount);

        // Search the twitterAccount
        restTwitterAccountMockMvc.perform(get("/api/_search/twitter-accounts?query=id:" + twitterAccount.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(twitterAccount.getId().intValue())))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL.toString())))
            .andExpect(jsonPath("$.[*].consumerKey").value(hasItem("")))
            .andExpect(jsonPath("$.[*].consumerSecret").value(hasItem("")))
            .andExpect(jsonPath("$.[*].accessToken").value(hasItem("")))
            .andExpect(jsonPath("$.[*].accessTokenSecret").value(hasItem("")))
            .andExpect(jsonPath("$.[*].username").value(hasItem(DEFAULT_USERNAME.toString())))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].prevStatus").value(hasItem(DEFAULT_PREV_STATUS.toString())));
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(TwitterAccount.class);
        TwitterAccount twitterAccount1 = new TwitterAccount();
        twitterAccount1.setId(1L);
        TwitterAccount twitterAccount2 = new TwitterAccount();
        twitterAccount2.setId(twitterAccount1.getId());
        assertThat(twitterAccount1).isEqualTo(twitterAccount2);
        twitterAccount2.setId(2L);
        assertThat(twitterAccount1).isNotEqualTo(twitterAccount2);
        twitterAccount1.setId(null);
        assertThat(twitterAccount1).isNotEqualTo(twitterAccount2);
    }
}
