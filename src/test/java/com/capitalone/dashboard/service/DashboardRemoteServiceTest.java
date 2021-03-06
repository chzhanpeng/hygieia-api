package com.capitalone.dashboard.service;

import com.capitalone.dashboard.config.ApiTestConfig;
import com.capitalone.dashboard.config.MongoServerConfig;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.AuthType;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.model.Owner;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.DashboardRepository;
import com.capitalone.dashboard.repository.UserInfoRepository;
import com.capitalone.dashboard.request.DashboardRemoteRequest;
import com.capitalone.dashboard.testutil.GsonUtil;
import com.capitalone.dashboard.util.TestUtil;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApiTestConfig.class, MongoServerConfig.class})
@DirtiesContext
public class DashboardRemoteServiceTest {

    @Autowired
    private DashboardRepository dashboardRepository;
    @Autowired
    private  UserInfoRepository userInfoRepository;
    @Autowired
    private  CollectorRepository collectorRepository;
    @Autowired
    private  ComponentRepository componentRepository;
    @Autowired
    private  CollectorItemRepository collectorItemRepository;
    @Autowired
    private  DashboardService dashboardService;
    @Autowired
    private  DashboardRemoteService dashboardRemoteService;

    @Before
    public  void loadStuff() throws IOException {
        TestUtil.loadDashBoard(dashboardRepository);
        TestUtil.loadUserInfo(userInfoRepository);
        TestUtil.loadCollector(collectorRepository);
        TestUtil.loadComponent(componentRepository);
        TestUtil.loadCollectorItems(collectorItemRepository);
    }

    @After
    public  void clearStuff() throws IOException {
        dashboardRepository.deleteAll();
        userInfoRepository.deleteAll();
        collectorRepository.deleteAll();
        componentRepository.deleteAll();
        collectorItemRepository.deleteAll();
    }

    @Test
    public void remoteUpdateCodeRepo() throws IOException, HygieiaException {

        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/0-Remote-Update-Repo.json");

        dashboardRemoteService.remoteCreate(request, true);
        List<Dashboard> dashboard = dashboardService.getByTitle("TestSSA");
        Component component = componentRepository.findOne(dashboard.get(0).getApplication().getComponents().get(0).getId());
        assertEquals(2, component.getCollectorItems().get(CollectorType.SCM).size());
    }
    @Test
    public void remoteCreateEmptyEntry() throws IOException, HygieiaException {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/Remote-Request-Base.json");
        Dashboard dashboard = dashboardRemoteService.remoteCreate(request, false);
        assertNotNull(dashboard);
        assertEquals(request.getMetaData().getTitle(),dashboard.getTitle());
    }
    @Test
    public void remoteCreateInvalidUser() throws IOException {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/0-Remote-Update-Repo.json");
        Owner owner = new Owner("test123",AuthType.STANDARD);
        request.getMetaData().setOwner(owner);
                Throwable t = new Throwable();
        RuntimeException excep = new RuntimeException("Invalid owner information or authentication type. Owner first needs to sign up in Hygieia", t);

        try {
            dashboardRemoteService.remoteCreate(request, false);
            fail("Should throw RuntimeException");
        } catch(Exception e) {
            assertEquals(excep.getMessage(), e.getMessage());
        }
    }
    @Test
    public void remoteCreateInvalidApp() throws IOException {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/0-Remote-Update-Repo.json");
        request.getMetaData().setTitle("test1234");
        request.getMetaData().setBusinessService("test");
        Throwable t = new Throwable();
        RuntimeException excep = new RuntimeException("Invalid Business Service Name.", t);

        try {
            dashboardRemoteService.remoteCreate(request, false);
            fail("Should throw RuntimeException");
        } catch(Exception e) {
            assertEquals(excep.getMessage(), e.getMessage());
        }
    }
    @Test
    public void remoteCreateInvalidComp() throws IOException {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/0-Remote-Update-Repo.json");
        request.getMetaData().setTitle("test1234");
        request.getMetaData().setBusinessApplication("test");
        Throwable t = new Throwable();
        RuntimeException excep = new RuntimeException("Invalid Business Application Name.", t);

        try {
            dashboardRemoteService.remoteCreate(request, false);
            fail("Should throw RuntimeException");
        } catch(Exception e) {
            assertEquals(excep.getMessage(), e.getMessage());
        }
    }
    @Test
    public void remoteCreateInvalidCompAndApp() throws IOException {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/0-Remote-Update-Repo.json");
        request.getMetaData().setTitle("test1234");
        request.getMetaData().setBusinessApplication("test");
        request.getMetaData().setBusinessService("test1");
        Throwable t = new Throwable();
        RuntimeException excep = new RuntimeException("Invalid Business Application Name.", t);

        try {
            dashboardRemoteService.remoteCreate(request, false);
            fail("Should throw RuntimeException");
        } catch(Exception e) {
            assertEquals(excep.getMessage(), e.getMessage());
        }
    }
    @Test
    public void remoteCreateDuplicateDashboard() throws IOException {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/0-Remote-Update-Repo.json");
        Dashboard dashboard = dashboardRepository.findByTitle(request.getMetaData().getTitle()).get(0);
        Throwable t = new Throwable();
        RuntimeException excep = new RuntimeException("Dashboard "+dashboard.getTitle()+" (id =" + dashboard.getId() + ") already exists", t);

        try {
            dashboardRemoteService.remoteCreate(request, false);
            fail("Should throw RuntimeException");
        } catch(Exception e) {
            assertEquals(excep.getMessage(), e.getMessage());
        }
    }
    @Test
    public void remoteCreate() throws HygieiaException, IOException  {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/0-Remote-Update-Repo.json");
        request.getMetaData().setTitle("newDashboard0");
        assertNotNull(dashboardRemoteService.remoteCreate(request, false));
    }
    @Test
    public void remoteCreateWithoutAppAndComp() throws HygieiaException, IOException  {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/0-Remote-Update-Repo.json");
        request.getMetaData().setTitle("newDashboard1");
        request.getMetaData().setBusinessApplication("");
        request.getMetaData().setBusinessService("");
        assertNotNull(dashboardRemoteService.remoteCreate(request, false));
    }

    @Test
    public void remoteCreateWithInvalidCollector() throws IOException {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/0-Remote-Update-Repo.json");
        request.getMetaData().setTitle("newDashboard2");
        List<DashboardRemoteRequest.CodeRepoEntry> entries = new ArrayList<>();

        DashboardRemoteRequest.CodeRepoEntry invalidSCM = new DashboardRemoteRequest.CodeRepoEntry();
        invalidSCM.setToolName("Clearcase");
        entries.add(invalidSCM);
        request.setCodeRepoEntries(entries);

        Throwable t = new Throwable();
        RuntimeException excep = new RuntimeException(invalidSCM.getToolName() + " collector is not available.", t);

        try {
            dashboardRemoteService.remoteCreate(request, false);
            fail("Should throw RuntimeException");
        } catch(Exception e) {
            assertEquals(excep.getMessage(), e.getMessage());
        }
    }

    @Test
    public void remoteCreateSCM() throws HygieiaException, IOException  {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/Remote-Request-Base.json");
        request.getMetaData().setTitle("newDashboard3");

        List<DashboardRemoteRequest.CodeRepoEntry> entries = new ArrayList<>();
        DashboardRemoteRequest.CodeRepoEntry validSCM = new DashboardRemoteRequest.CodeRepoEntry();
        validSCM.setToolName("GitHub");
        Map options = new HashMap();
        options.put("url", "http://git.test.com/capone/better.git");
        options.put("branch", "master");
        validSCM.setOptions(options);
        entries.add(validSCM);
        request.setCodeRepoEntries(entries);

        dashboardRemoteService.remoteCreate(request, false);
        List<Dashboard> dashboard = dashboardService.getByTitle("newDashboard3");
        Component component = componentRepository.findOne(dashboard.get(0).getApplication().getComponents().get(0).getId());
        assertEquals(1, component.getCollectorItems().get(CollectorType.SCM).size());

    }

    @Test
    public void remoteCreateBuild() throws HygieiaException, IOException {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/Remote-Request-Base.json");
        request.getMetaData().setTitle("newDashboard4");
        List<DashboardRemoteRequest.BuildEntry> entries = new ArrayList<>();
        DashboardRemoteRequest.BuildEntry validBuild = new DashboardRemoteRequest.BuildEntry();
        validBuild.setToolName("Hudson");
        Map options = new HashMap();
        options.put("jobName", "MyBuildJob");
        options.put("jobUrl", "http://jenkins.com/MyBuildJob");
        options.put("instanceUrl", "http://jenkins.com");
        validBuild.setOptions(options);
        entries.add(validBuild);
        request.setBuildEntries(entries);

        dashboardRemoteService.remoteCreate(request, false);
        List<Dashboard> dashboard = dashboardService.getByTitle("newDashboard4");
        Component component = componentRepository.findOne(dashboard.get(0).getApplication().getComponents().get(0).getId());
        assertEquals(1, component.getCollectorItems().get(CollectorType.Build).size());
    }
    @Test
    public void remoteUpdateNonExisting() throws IOException {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/Remote-Request-Base.json");
        request.getMetaData().setTitle("missingDashboard");

        Throwable t = new Throwable();
        RuntimeException excep = new RuntimeException("Dashboard " + request.getMetaData().getTitle() +  " does not exist.", t);

        try {
            dashboardRemoteService.remoteCreate(request, true);
            fail("Should throw RuntimeException");
        } catch(Exception e) {
            assertEquals(excep.getMessage(), e.getMessage());
        }
    }
    @Test
    public void remoteUpdateLibraryScan() throws HygieiaException, IOException {
        DashboardRemoteRequest request = getRemoteRequest("./dashboardRemoteRequests/Remote-Request-Base.json");
        request.getMetaData().setTitle("TestSSA");
        List<DashboardRemoteRequest.LibraryScanEntry> entries = new ArrayList<>();
        DashboardRemoteRequest.LibraryScanEntry validLibraryScan = new DashboardRemoteRequest.LibraryScanEntry();
        validLibraryScan.setToolName("NexusIQ");
        Map options = new HashMap();
        options.put("applicationId", "applicationIdTest");
        options.put("applicationName", "testApplicationName");
        options.put("publicId", "123456");
        options.put("instanceUrl", "http://test.com");
        validLibraryScan.setOptions(options);
        entries.add(validLibraryScan);

        validLibraryScan = new DashboardRemoteRequest.LibraryScanEntry();
        validLibraryScan.setToolName("NexusIQ");
        options = new HashMap();
        options.put("applicationId", "applicationIdTest1");
        options.put("applicationName", "testApplicationName1");
        options.put("publicId", "1234561");
        options.put("instanceUrl", "http://test1.com");
        validLibraryScan.setOptions(options);
        entries.add(validLibraryScan);

        request.setLibraryScanEntries(entries);

        dashboardRemoteService.remoteCreate(request, true);
        List<Dashboard> dashboard = dashboardService.getByTitle("TestSSA");
        Component component = componentRepository.findOne(dashboard.get(0).getApplication().getComponents().get(0).getId());
        assertEquals(2, component.getCollectorItems().get(CollectorType.LibraryPolicy).size());
        assertNotNull(component.getCollectorItems().get(CollectorType.CodeQuality));
        assertNotNull(component.getCollectorItems().get(CollectorType.Test));
        assertNotNull(component.getCollectorItems().get(CollectorType.StaticSecurityScan));

    }

    private static String getExpectedJSON(String path) throws IOException {
        URL fileUrl = Resources.getResource(path);
        return IOUtils.toString(fileUrl);
    }
    private static DashboardRemoteRequest getRemoteRequest(String fileName) throws IOException {
        Gson gson = GsonUtil.getGson();
        return gson.fromJson(getExpectedJSON(fileName), new TypeToken<DashboardRemoteRequest>(){}.getType());
    }
}
