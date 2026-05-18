package com.example.demo.integration;

import com.example.demo.integration.config.ZeebeTestConfig;
import com.example.demo.integration.fixture.LeaveRequestFixture;
import com.example.demo.integration.helper.DbTestHelper;
import com.example.demo.integration.helper.ZeebeTestHelper;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * BaseIntegrationTest: Lớp cơ sở cho tất cả Integration Test.
 *
 * Trách nhiệm:
 * - Deploy BPMN + DMN lên Zeebe cluster một lần duy nhất trước khi chạy toàn bộ bộ test
 *   (dùng @BeforeAll + @TestInstance(PER_CLASS))
 * - Inject các bean dùng chung
 *
 * Các test class KẾ THỪA lớp này được và không bắt buộc phải kế thừa.
 * Nếu không kế thừa: mỗi test class phải tự @Import(ZeebeTestConfig.class)
 * và tự deploy nếu cần.
 *
 * LƯU Ý VỀ DEPLOY:
 * Zeebe sẽ tạo phiên bản mới mỗi khi deploy, và auto-route process instance
 * đến phiên bản mới nhất. Nếu BPMN/DMN không thay đổi, Zeebe sẽ bỏ qua
 * (idempotent deploy bằng cách so sánh checksum).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ZeebeTestConfig.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    @Autowired
    protected ZeebeTestHelper zeebeHelper;

    @Autowired
    protected DbTestHelper dbHelper;

    @Autowired
    protected ZeebeClient zeebeClient;

    /**
     * Deploy BPMN và DMN lên Zeebe cluster một lần trước khi chạy tất cả tests.
     *
     * BPMN: process.bpmn (Process ID: Process_1oveniu)
     * DMN:  dnn.dmn (Decision ID: rule-base-0mgnq1p)
     *
     * CÁC FILE NÀY PHẢI ĐẶT TRONG:
     *   src/test/resources/processes/project.bpmn
     *   src/test/resources/processes/dnn.dmn
     */
    @BeforeAll
    void deployProcesses() {
        System.out.println("[TEST-SETUP] Deploying BPMN and DMN resources to Zeebe cluster...");
        try {
            DeploymentEvent deployment = zeebeHelper.deployResources(
                    "processes/project.bpmn",
                    "processes/dnn.dmn"
            );
            System.out.println("[TEST-SETUP] Deployment successful. Key: " + deployment.getKey());
        } catch (Exception e) {
            System.err.println("[TEST-SETUP] WARNING: Could not deploy resources: " + e.getMessage());
            System.err.println("[TEST-SETUP] Tests will use already-deployed versions if available.");
        }
    }
}
