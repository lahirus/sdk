/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.cellery.components.test;

import io.cellery.models.Cell;
import org.ballerinax.kubernetes.exceptions.KubernetesPluginException;
import org.ballerinax.kubernetes.utils.KubernetesUtils;
import org.cellery.components.test.utils.CelleryUtils;
import org.cellery.components.test.utils.LangTestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.cellery.components.test.utils.CelleryTestConstants.CELLERY;
import static org.cellery.components.test.utils.CelleryTestConstants.CELLERY_IMAGE_NAME;
import static org.cellery.components.test.utils.CelleryTestConstants.CELLERY_IMAGE_ORG;
import static org.cellery.components.test.utils.CelleryTestConstants.CELLERY_IMAGE_VERSION;
import static org.cellery.components.test.utils.CelleryTestConstants.EMPLOYEE_PORTAL;
import static org.cellery.components.test.utils.CelleryTestConstants.TARGET;

public class StockTest {
    private static final Path SAMPLE_DIR = Paths.get(System.getProperty("sample.dir"));
    private static final Path SOURCE_DIR_PATH = SAMPLE_DIR.resolve(EMPLOYEE_PORTAL + File.separator + CELLERY +
            File.separator + "stock");
    private static final Path TARGET_PATH = SOURCE_DIR_PATH.resolve(TARGET);
    private static final Path CELLERY_PATH = TARGET_PATH.resolve(CELLERY);
    private Cell cell;

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        String imgData = "{\"org\":\"wso2\", \"name\":\"stockImg\", \"ver\":\"1.0.0\"}";
        Assert.assertEquals(LangTestUtils.compileCellBuildFunction(SOURCE_DIR_PATH, "stocks.bal", imgData), 0);
        File artifactYaml = CELLERY_PATH.resolve("stockImg.yaml").toFile();
        Assert.assertTrue(artifactYaml.exists());
        cell = CelleryUtils.getInstance(CELLERY_PATH.resolve("stockImg.yaml").toString());
    }

    @Test
    public void validateCellAvailability() {
        Assert.assertNotNull(cell);
    }

    @Test
    public void validateAPIVersion() {
        Assert.assertEquals(cell.getApiVersion(), "mesh.cellery.io/v1alpha1");
    }

    @Test
    public void validateMetaData() {
        Assert.assertEquals(cell.getMetadata().getName(), "stockimg");
        Assert.assertEquals(cell.getMetadata().getAnnotations().get(CELLERY_IMAGE_ORG),
                "wso2");
        Assert.assertEquals(cell.getMetadata().getAnnotations().get(CELLERY_IMAGE_NAME),
                "stockImg");
        Assert.assertEquals(cell.getMetadata().getAnnotations().get(CELLERY_IMAGE_VERSION),
                "1.0.0");
    }

    @Test
    public void validateGatewayTemplate() {
        Assert.assertEquals(cell.getSpec().getGatewayTemplate().getSpec().getHttp().get(0).getBackend(), "stock");
        Assert.assertEquals(cell.getSpec().getGatewayTemplate().getSpec().getHttp().get(0).getContext(), "stock");
        Assert.assertEquals(cell.getSpec().getGatewayTemplate().getSpec().getHttp().get(0).getDefinitions().get(0).
                getMethod(), "GET");
        Assert.assertEquals(cell.getSpec().getGatewayTemplate().getSpec().getHttp().get(0).getDefinitions().get(0).
                getPath(), "/options");
    }

    @Test
    public void validateServiceTemplates() {
        Assert.assertEquals(cell.getSpec().getServicesTemplates().get(0).getMetadata().getName(), "stock");
        Assert.assertEquals(cell.getSpec().getServicesTemplates().get(0).getSpec().getContainer().getImage(),
                "docker.io/celleryio/sampleapp-stock");
        Assert.assertEquals(cell.getSpec().getServicesTemplates().get(0).getSpec().getContainer().getPorts().get(0).
                getContainerPort().intValue(), 8080);
        Assert.assertEquals(cell.getSpec().getServicesTemplates().get(0).getSpec().getReplicas(), 1);
        Assert.assertEquals(cell.getSpec().getServicesTemplates().get(0).getSpec().getServicePort(), 80);
    }

    @AfterClass
    public void cleanUp() throws KubernetesPluginException {
        KubernetesUtils.deleteDirectory(String.valueOf(TARGET_PATH));
    }
}

