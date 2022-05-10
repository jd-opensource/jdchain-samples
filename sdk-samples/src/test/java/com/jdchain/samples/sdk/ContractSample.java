package com.jdchain.samples.sdk;

import com.jd.blockchain.crypto.*;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.transaction.ContractEventSendOperationBuilder;
import com.jd.blockchain.transaction.ContractReturnValue;
import com.jd.blockchain.transaction.GenericValueHolder;
import com.jdchain.samples.contract.SampleContract;
import org.junit.Assert;
import org.junit.Test;
import utils.codec.Base58Utils;
import utils.crypto.adv.ShamirUtils;
import utils.io.BytesUtils;
import utils.io.FileUtils;
import utils.serialize.json.JSONSerializeUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * 合约相关操作示例：
 * 合约部署，合约调用
 */
public class ContractSample extends SampleBase {

    /**
     * 有两种方式部署合约：
     * 1. contract-samples模块下，配置好pom里面的参数，执行 mvn clean deploy 即可
     * 2. 打包contract-samples项目生成 car包，参考testDeploy测试代码部署
     */
    @Test
    public void testDeploy() {
        // 新建交易
        TransactionTemplate txTemp = blockchainService.newTransaction(ledger);
        // 生成合约账户
        BlockchainKeypair contractAccount = BlockchainKeyGenerator.getInstance().generate();
        System.out.println("合约地址：" + contractAccount.getAddress());
        // 部署合约
        txTemp.contracts().deploy(contractAccount.getIdentity(), FileUtils.readBytes("src/test/resources/contract-samples-1.6.4.RELEASE.car"));
        // 准备交易
        PreparedTransaction ptx = txTemp.prepare();
        // 提交交易
        TransactionResponse response = ptx.commit();
        Assert.assertTrue(response.isSuccess());
    }

    /**
     * 有两种方式更新合约代码：
     * 1. contract-samples模块下，配置好pom里面的参数，其中contractAddress设置为已部署上链合约公钥信息，执行 mvn clean deploy 即可
     * 2. 打包contract-samples项目生成 car包，参考testUpdate测试代码部署
     */
    @Test
    public void testUpdate() {
        // 新建交易
        TransactionTemplate txTemp = blockchainService.newTransaction(ledger);
        // 解析合约身份信息
        BlockchainIdentity contractIdentity = new BlockchainIdentityData(KeyGenUtils.decodePubKey("7VeRL4K4bLP5Jodt748dEnfe2UrqfNLQAWnBzMDoRt25KYnr"));
        System.out.println("合约地址：" + contractIdentity.getAddress());
        // 部署合约
        txTemp.contracts().deploy(contractIdentity, FileUtils.readBytes("src/test/resources/contract-samples-1.6.4.RELEASE.car"));
        // 准备交易
        PreparedTransaction ptx = txTemp.prepare();
        // 提交交易
        TransactionResponse response = ptx.commit();
        Assert.assertTrue(response.isSuccess());
    }

    /**
     * 基于动态代理方式合约调用，需要依赖合约接口
     */
    @Test
    public void testExecuteByProxy() {
        // 新建交易
        TransactionTemplate txTemp = blockchainService.newTransaction(ledger);

        // 运行前，填写正确的合约地址
        // 一次交易中可调用多个（多次调用）合约方法
        // 调用合约的 registerUser 方法
        SampleContract sampleContract = txTemp.contract("LdeNrX4fMAwwbbLRZu4jKp7AxPkJ7wY9LSws8", SampleContract.class);
        GenericValueHolder<String> userAddress = ContractReturnValue.decode(sampleContract.registerUser(UUID.randomUUID().toString()));

        // 准备交易
        PreparedTransaction ptx = txTemp.prepare();
        // 提交交易
        TransactionResponse response = ptx.commit();
        Assert.assertTrue(response.isSuccess());

        // 获取返回值
        System.out.println(userAddress.get());
    }

    /**
     * 非动态代理方式合约调用，不需要依赖合约接口及实现
     */
    @Test
    public void testExecuteWithArgus() {
        // 新建交易
        TransactionTemplate txTemp = blockchainService.newTransaction(ledger);

        ContractEventSendOperationBuilder builder = txTemp.contract("LdeNrX4fMAwwbbLRZu4jKp7AxPkJ7wY9LSws8");
        // 运行前，填写正确的合约地址，数据账户地址等参数
        // 一次交易中可调用多个（多次调用）合约方法
        // 调用合约的 registerUser 方法，传入合约地址，合约方法名，合约方法参数列表
        builder.invoke("registerUser", new BytesDataList(new TypedValue[]{TypedValue.fromText(UUID.randomUUID().toString())}), 0);// 此处可指定具体合约版本，不指定时传-1表示当前最新版本
        // 准备交易
        PreparedTransaction ptx = txTemp.prepare();
        // 提交交易
        TransactionResponse response = ptx.commit();
        Assert.assertTrue(response.isSuccess());

        Assert.assertEquals(1, response.getOperationResults().length);
        // 解析合约方法调用返回值
        for (int i = 0; i < response.getOperationResults().length; i++) {
            BytesValue content = response.getOperationResults()[i].getResult();
            switch (content.getType()) {
                case TEXT:
                case JSON:
                    System.out.println(content.getBytes().toUTF8String());
                    break;
                case INT64:
                    System.out.println(BytesUtils.toLong(content.getBytes().toBytes()));
                    break;
                case BOOLEAN:
                    System.out.println(BytesUtils.toBoolean(content.getBytes().toBytes()[0]));
                    break;
                default: // byte[], Bytes
                    System.out.println(content.getBytes().toBase58());
                    break;
            }
        }
    }

    /**
     * 更新合约状态
     */
    @Test
    public void updateContractState() {
        // 新建交易
        TransactionTemplate txTemp = blockchainService.newTransaction(ledger);
        // 合约状态分为：NORMAL（正常） FREEZE（冻结） REVOKE（销毁）
        // 冻结合约
        txTemp.contract("LdeNrX4fMAwwbbLRZu4jKp7AxPkJ7wY9LSws8").state(AccountState.FREEZE);
        // 交易准备
        PreparedTransaction ptx = txTemp.prepare();
        // 提交交易
        TransactionResponse response = ptx.commit();
        Assert.assertTrue(response.isSuccess());
    }

    /**
     * 更新合约权限
     */
    @Test
    public void updateDPermission() {
        // 新建交易
        TransactionTemplate txTemp = blockchainService.newTransaction(ledger);
        // 配置合约权限
        // 如下配置表示仅有 ROLE 角色用户才有调用 LdeNr7H1CUbqe3kWjwPwiqHcmd86zEQz2VRye 权限
        txTemp.contract("LdeNrX4fMAwwbbLRZu4jKp7AxPkJ7wY9LSws8").permission().mode(70).role("ROLE");
        // 交易准备
        PreparedTransaction ptx = txTemp.prepare();
        // 提交交易
        TransactionResponse response = ptx.commit();
        Assert.assertTrue(response.isSuccess());
    }

    /**
     * 同态加法
     */
    @Test
    public void paillierAdd() {
        PubKey pubKey = KeyGenUtils.decodePubKey("EEXnycK4ZXGmLueDsk6AcWBWqHW8qwbchF3VYL4mjNeNxnk8G6zdjewR6Z44nJiwTzGcRdviSYUaq9xVBBR1QSX5DDGmgsEDiCjcsnxbfEhibji8WUL2Qot7puxUuYNyfauymYDCMBVQq1NJZaZRNgN48rHSejf7zN6KTDwPNycYyM8SWs5j5Ueko2BNNBipKvhk5aUMWr1MyjztHZPw64a5XmPcA9ac8RNtf2ENKJakN1qfmDFs84i5rdz6qNt3YKm9Mqoou5qzzRxs1Y72ZwB5HPmE3pTWA4e6EMf7AtXYxGZyZJC6agRtgTDJrN7yc2zfULFSoxZWoXZVfd3xj8YHnrkjHf8ssr");
        PrivKey privKey = KeyGenUtils.decodePrivKey("1wAyFwoRocuYDxBRDoSE3dSS84RZ8fpKzdzvqipjNwFtMGPJVNb2KYYVMQjQZuzTSn8iESttQJmzhPXbhwSZyeU69vV5d1fR5Pz4gNfw9YAudxpAR3g1Tzr2H7cc8ygrZ8685Z2sTSYQbCkt2DuipTbjnVCEY6uG7jSAPxtYSky7AvZyF9Z8tNncoM3yBuHYDafP7gXG9DaLTEydercCBW5tTdW8MZEdmeng2QzPPPP6bK4NVfjUjMfEhCDd5jRxHB5hefa2r1aM6WdJjSWhwpyNAQUPWxQdfcq21gBfQDcFZpPAgWWcv6HDBVNgwxxWWvrkArRHZzA6bkpZ1BV9GqhyLRF9JZTJYZexGnm1tn4XT6To1Xzz2NtgtorbtiWK91hvcDDvS2cTuAzxUMic52muWEyz3Nuouma915dFGqHHiSt2DWRgyYTRTEM5DfZoxJoS1m22qkN148eCDzLxsVMua3p38vxNnAjGqpSEu5WKsRwZ1e9e75DNe3j8nKRFmry6772NEKBaHWCFctFNHPWNRd77ZSELa9VqM96tPP7Ai1AfWLZ4R5MaYtxoEZLVFPwzp6a5PzfER2Tkt3BM8DTuqQZUwjmxJQKKBAWRtctq57DzJydivw7xT3sceEW649LJLfBzuvqRDwJuDfpvv1SXL5PgJPtXCNS5FKnc8cuSZPPJNFJ6X5qbJ8ifAkV35hvdZdtuzo4fdSVuEBRpQt2EUmkLV6XFAtFogZA5q26a64XebTqBFHLWriL5w62M5cBc5VyKsarkhYoPc8Sc6J9sMF1ScoAud51qr5e6EzZ9xoHrZupDkoQzTPARgjGXww8hAfzi9YSBeR27eRWTbjdT8p7ekmkv3ciKxrmtrzw3sAsuPQFQs34cJWWTmQjAVD25iZvefeLMq4keJrgfSnGmkCVtA2omxJsXT5V2zdmsziVCzK9GPk56VpqwbKGy7fb6RXhJijCLULxUzgAdme2yfXXE2v43ggQ2QxQhZ5kAuRJTh67ko7keH5jq78pZKSoSsdVZp17peTtf1r6Mk4fvCqCySdnJjbQHejYoDQLCksWbqDcSwj4BgnRtUVqQniMW3ibBiffc2vmJSYL8mr49oMiKRDPGvFnYPqnsuiR13xBpXXHXfDWYFmT7j3UDqxQW5YfebEKNHYTbgkx6zrF3nJxYVYe6j7Gg4fB9Fo5g87e7vLFRPAz6upMX8tWTcTd1F8bVrwiYsEh3rzcHeAh3exsPQYjEiYoZNXAdYDfkUD5YR8fuGNmSUCHYgWbUSZfBufZWKKiJHrLVVi5uHge5TvgN7EvXGJBrys8K1m2ufrYgBzBYYNG2dNMqRvWXXtR8U34xDDccw3Ht5LjVWLQQ59RyBPSX8a2ZpU1wMG3Yc1sjxVUPFj9E8aHpGP83v5MHpFZMAWsbrXuhAXpwyiQdc91X7wFiVKuGFMFdadXkvWaGNbQF2zxhVoYKks9qmoR64NxsjXHyAC3GHMZLMSHvvvVqhGtQu9eDo6LaFqQRAmVHbCH3eF4YvR1Zdv15bxFUR7323uoRcUyPhCzk3rbXcHcbBBQMjJsuEzsmj3M8eub5RdgdRmcQ1eLLg4GG", "RVu1HWU5");

        String contract = "LdeNrX4fMAwwbbLRZu4jKp7AxPkJ7wY9LSws8";

        int input1 = 600;
        int input2 = 60;
        int sum = 660;
        CryptoAlgorithm algorithm = Crypto.getAlgorithm("PAILLIER");
        HomomorphicCryptoFunction homomorphicCryptoFunction = (HomomorphicCryptoFunction) Crypto.getCryptoFunction(algorithm);
        byte[] ciphertext1 = homomorphicCryptoFunction.encrypt(pubKey, BytesUtils.toBytes(input1));
        byte[] ciphertext2 = homomorphicCryptoFunction.encrypt(pubKey, BytesUtils.toBytes(input2));
        // 调用合约
        TransactionTemplate txCall = blockchainService.newTransaction(ledger);
        txCall.contract(contract).invoke("paillierAdd", new BytesDataList(new TypedValue[]{TypedValue.fromText("EEXnycK4ZXGmLueDsk6AcWBWqHW8qwbchF3VYL4mjNeNxnk8G6zdjewR6Z44nJiwTzGcRdviSYUaq9xVBBR1QSX5DDGmgsEDiCjcsnxbfEhibji8WUL2Qot7puxUuYNyfauymYDCMBVQq1NJZaZRNgN48rHSejf7zN6KTDwPNycYyM8SWs5j5Ueko2BNNBipKvhk5aUMWr1MyjztHZPw64a5XmPcA9ac8RNtf2ENKJakN1qfmDFs84i5rdz6qNt3YKm9Mqoou5qzzRxs1Y72ZwB5HPmE3pTWA4e6EMf7AtXYxGZyZJC6agRtgTDJrN7yc2zfULFSoxZWoXZVfd3xj8YHnrkjHf8ssr"), TypedValue.fromText(Base58Utils.encode(ciphertext1)), TypedValue.fromText(Base58Utils.encode(ciphertext2))}));
        PreparedTransaction ptxCall = txCall.prepare();
        TransactionResponse response = ptxCall.commit();
        Assert.assertTrue(response.isSuccess());
        byte[] aggregatedCiphertext = Base58Utils.decode(response.getOperationResults()[0].getResult().getBytes().toUTF8String());
        byte[] plaintext = homomorphicCryptoFunction.decrypt(privKey, aggregatedCiphertext);
        int output = BytesUtils.toInt(plaintext);
        assertEquals(sum, output);
    }

    /**
     * 同态乘法
     */
    @Test
    public void paillierMul() {
        PubKey pubKey = KeyGenUtils.decodePubKey("EEXnycK4ZXGmLueDsk6AcWBWqHW8qwbchF3VYL4mjNeNxnk8G6zdjewR6Z44nJiwTzGcRdviSYUaq9xVBBR1QSX5DDGmgsEDiCjcsnxbfEhibji8WUL2Qot7puxUuYNyfauymYDCMBVQq1NJZaZRNgN48rHSejf7zN6KTDwPNycYyM8SWs5j5Ueko2BNNBipKvhk5aUMWr1MyjztHZPw64a5XmPcA9ac8RNtf2ENKJakN1qfmDFs84i5rdz6qNt3YKm9Mqoou5qzzRxs1Y72ZwB5HPmE3pTWA4e6EMf7AtXYxGZyZJC6agRtgTDJrN7yc2zfULFSoxZWoXZVfd3xj8YHnrkjHf8ssr");
        PrivKey privKey = KeyGenUtils.decodePrivKey("1wAyFwoRocuYDxBRDoSE3dSS84RZ8fpKzdzvqipjNwFtMGPJVNb2KYYVMQjQZuzTSn8iESttQJmzhPXbhwSZyeU69vV5d1fR5Pz4gNfw9YAudxpAR3g1Tzr2H7cc8ygrZ8685Z2sTSYQbCkt2DuipTbjnVCEY6uG7jSAPxtYSky7AvZyF9Z8tNncoM3yBuHYDafP7gXG9DaLTEydercCBW5tTdW8MZEdmeng2QzPPPP6bK4NVfjUjMfEhCDd5jRxHB5hefa2r1aM6WdJjSWhwpyNAQUPWxQdfcq21gBfQDcFZpPAgWWcv6HDBVNgwxxWWvrkArRHZzA6bkpZ1BV9GqhyLRF9JZTJYZexGnm1tn4XT6To1Xzz2NtgtorbtiWK91hvcDDvS2cTuAzxUMic52muWEyz3Nuouma915dFGqHHiSt2DWRgyYTRTEM5DfZoxJoS1m22qkN148eCDzLxsVMua3p38vxNnAjGqpSEu5WKsRwZ1e9e75DNe3j8nKRFmry6772NEKBaHWCFctFNHPWNRd77ZSELa9VqM96tPP7Ai1AfWLZ4R5MaYtxoEZLVFPwzp6a5PzfER2Tkt3BM8DTuqQZUwjmxJQKKBAWRtctq57DzJydivw7xT3sceEW649LJLfBzuvqRDwJuDfpvv1SXL5PgJPtXCNS5FKnc8cuSZPPJNFJ6X5qbJ8ifAkV35hvdZdtuzo4fdSVuEBRpQt2EUmkLV6XFAtFogZA5q26a64XebTqBFHLWriL5w62M5cBc5VyKsarkhYoPc8Sc6J9sMF1ScoAud51qr5e6EzZ9xoHrZupDkoQzTPARgjGXww8hAfzi9YSBeR27eRWTbjdT8p7ekmkv3ciKxrmtrzw3sAsuPQFQs34cJWWTmQjAVD25iZvefeLMq4keJrgfSnGmkCVtA2omxJsXT5V2zdmsziVCzK9GPk56VpqwbKGy7fb6RXhJijCLULxUzgAdme2yfXXE2v43ggQ2QxQhZ5kAuRJTh67ko7keH5jq78pZKSoSsdVZp17peTtf1r6Mk4fvCqCySdnJjbQHejYoDQLCksWbqDcSwj4BgnRtUVqQniMW3ibBiffc2vmJSYL8mr49oMiKRDPGvFnYPqnsuiR13xBpXXHXfDWYFmT7j3UDqxQW5YfebEKNHYTbgkx6zrF3nJxYVYe6j7Gg4fB9Fo5g87e7vLFRPAz6upMX8tWTcTd1F8bVrwiYsEh3rzcHeAh3exsPQYjEiYoZNXAdYDfkUD5YR8fuGNmSUCHYgWbUSZfBufZWKKiJHrLVVi5uHge5TvgN7EvXGJBrys8K1m2ufrYgBzBYYNG2dNMqRvWXXtR8U34xDDccw3Ht5LjVWLQQ59RyBPSX8a2ZpU1wMG3Yc1sjxVUPFj9E8aHpGP83v5MHpFZMAWsbrXuhAXpwyiQdc91X7wFiVKuGFMFdadXkvWaGNbQF2zxhVoYKks9qmoR64NxsjXHyAC3GHMZLMSHvvvVqhGtQu9eDo6LaFqQRAmVHbCH3eF4YvR1Zdv15bxFUR7323uoRcUyPhCzk3rbXcHcbBBQMjJsuEzsmj3M8eub5RdgdRmcQ1eLLg4GG", "RVu1HWU5");

        String contract = "LdeNrX4fMAwwbbLRZu4jKp7AxPkJ7wY9LSws8";

        int input1 = 600;
        int scalar = 10;
        int sum = 6000;
        CryptoAlgorithm algorithm = Crypto.getAlgorithm("PAILLIER");
        HomomorphicCryptoFunction homomorphicCryptoFunction = (HomomorphicCryptoFunction) Crypto.getCryptoFunction(algorithm);
        byte[] ciphertext1 = homomorphicCryptoFunction.encrypt(pubKey, BytesUtils.toBytes(input1));
        // 调用合约
        TransactionTemplate txCall = blockchainService.newTransaction(ledger);
        txCall.contract(contract).invoke("paillierMul", new BytesDataList(new TypedValue[]{TypedValue.fromText("EEXnycK4ZXGmLueDsk6AcWBWqHW8qwbchF3VYL4mjNeNxnk8G6zdjewR6Z44nJiwTzGcRdviSYUaq9xVBBR1QSX5DDGmgsEDiCjcsnxbfEhibji8WUL2Qot7puxUuYNyfauymYDCMBVQq1NJZaZRNgN48rHSejf7zN6KTDwPNycYyM8SWs5j5Ueko2BNNBipKvhk5aUMWr1MyjztHZPw64a5XmPcA9ac8RNtf2ENKJakN1qfmDFs84i5rdz6qNt3YKm9Mqoou5qzzRxs1Y72ZwB5HPmE3pTWA4e6EMf7AtXYxGZyZJC6agRtgTDJrN7yc2zfULFSoxZWoXZVfd3xj8YHnrkjHf8ssr"), TypedValue.fromText(Base58Utils.encode(ciphertext1)), TypedValue.fromInt32(scalar)}), 0);
        PreparedTransaction ptxCall = txCall.prepare();
        TransactionResponse response = ptxCall.commit();
        Assert.assertTrue(response.isSuccess());
        byte[] aggregatedCiphertext = Base58Utils.decode(response.getOperationResults()[0].getResult().getBytes().toUTF8String());
        byte[] plaintext = homomorphicCryptoFunction.decrypt(privKey, aggregatedCiphertext);
        int output = BytesUtils.toInt(plaintext);
        assertEquals(sum, output);
    }

    /**
     * Shamir's Secret
     */
    @Test
    public void shamirSecret() {
        String secret = "JD Chain";
        String[] parts = ShamirUtils.split(5, 3, secret.getBytes(StandardCharsets.UTF_8));

        // 新建交易
        TransactionTemplate txTemp = blockchainService.newTransaction(ledger);
        ContractEventSendOperationBuilder builder = txTemp.contract("LdeNp4QBheB9Uw4upH29mjZET5FeWqWfCoaE1");
        // 运行前，填写正确的合约地址，数据账户地址等参数
        builder.invoke("secretRecover", new BytesDataList(new TypedValue[]{TypedValue.fromText(JSONSerializeUtils.serializeToJSON(parts))}), 0);// 此处可指定具体合约版本，不指定时传-1表示当前最新版本
        // 准备交易
        PreparedTransaction ptx = txTemp.prepare();
        // 提交交易
        TransactionResponse response = ptx.commit();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(1, response.getOperationResults().length);

        // 验证秘密
        Assert.assertEquals(secret, new String(response.getOperationResults()[0].getResult().getBytes().toBytes()));
    }
}
