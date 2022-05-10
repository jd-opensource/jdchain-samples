package com.jdchain.samples.contract;

import com.jd.blockchain.contract.Contract;
import com.jd.blockchain.contract.ContractEvent;

/**
 * 合约样例，提供通过合约创建用户/数据账户/事件账户，写入KV，发布事件等功能
 */
@Contract
public interface SampleContract {

    /**
     * 注册用户
     *
     * @param seed 种子，不小于32个字符
     */
    @ContractEvent(name = "registerUser")
    String registerUser(String seed);

    /**
     * 使用公钥注册用户
     *
     * @param pubkey
     */
    @ContractEvent(name = "registerUserByPubKey")
    void registerUserByPubKey(String pubkey);

    /**
     * 创建角色，并分配权限
     *
     * @param role
     * @param ledgerPermissions
     * @param txPermissions
     */
    @ContractEvent(name = "createRoleAndPermissions")
    void createRoleAndPermissions(String role, String ledgerPermissions, String txPermissions);

    /**
     * 修改用户角色
     *
     * @param address
     * @param role
     */
    @ContractEvent(name = "modifyUserRole")
    void modifyUserRole(String address, String role);

    /**
     * 修改用户状态
     *
     * @param userAddress
     * @param state
     */
    @ContractEvent(name = "modifyUserState")
    void modifyUserState(String userAddress, String state);

    /**
     * 注册数据账户
     *
     * @param seed 种子，不小于32个字符
     */
    @ContractEvent(name = "registerDataAccount")
    String registerDataAccount(String seed);

    /**
     * 修改数据账户角色及mode
     *
     * @param dataAccountAddress
     * @param role
     * @param mode
     */
    @ContractEvent(name = "modifyDataAccountRoleAndMode")
    void modifyDataAccountRoleAndMode(String dataAccountAddress, String role, String mode);

    /**
     * 设置KV
     *
     * @param address 数据账户地址
     * @param key     键
     * @param value   值
     * @param version 版本
     */
    @ContractEvent(name = "setKVWithVersion")
    void setKVWithVersion(String address, String key, String value, long version);

    /**
     * 设置KV，基于最新数据版本
     *
     * @param address 数据账户地址
     * @param key     键
     * @param value   值
     */
    @ContractEvent(name = "setKV")
    void setKV(String address, String key, String value);

    /**
     * 注册事件账户
     *
     * @param seed 种子，不小于32个字符
     */
    @ContractEvent(name = "registerEventAccount")
    String registerEventAccount(String seed);

    /**
     * 修改事件账户角色及mode
     *
     * @param eventAccountAddress
     * @param role
     * @param mode
     */
    @ContractEvent(name = "modifyEventAccountRoleAndMode")
    void modifyEventAccountRoleAndMode(String eventAccountAddress, String role, String mode);

    /**
     * 发布事件
     *
     * @param address  事件账户地址
     * @param topic    消息名称
     * @param content  内容
     * @param sequence 当前消息名称下最大序号（初始为-1）
     */
    @ContractEvent(name = "publishEventWithSequence")
    void publishEventWithSequence(String address, String topic, String content, long sequence);

    /**
     * 发布事件，基于最新时间序号
     *
     * @param address 事件账户地址
     * @param topic   消息名称
     * @param content 内容
     */
    @ContractEvent(name = "publishEvent")
    void publishEvent(String address, String topic, String content);

    /**
     * 合约中调用合约
     *
     * @param contractAddress
     * @param method
     * @param argDotStr
     */
    @ContractEvent(name = "invokeContract")
    void invokeContract(String contractAddress, String method, String argDotStr);

    /**
     * 合约中部署合约
     *
     * @param pubkey
     * @param carBytes
     * @return
     */
    @ContractEvent(name = "deployContract")
    String deployContract(String pubkey, byte[] carBytes);

    /**
     * 修改合约角色及mode
     *
     * @param contractAddress
     * @param role
     * @param mode
     */
    @ContractEvent(name = "modifyContractRoleAndMode")
    void modifyContractRoleAndMode(String contractAddress, String role, String mode);

    /**
     * 修改合约状态
     *
     * @param contractAddress
     * @param state
     */
    @ContractEvent(name = "modifyContractState")
    void modifyContractState(String contractAddress, String state);

    /**
     * 同态加法
     *
     * @param pubkey
     * @param cipher1
     * @param cipher2
     * @return
     */
    @ContractEvent(name = "paillierAdd")
    String paillierAdd(String pubkey, String cipher1, String cipher2);

    /**
     * 同态乘法
     *
     * @param pubkey
     * @param cipher
     * @param scalar
     * @return
     */
    @ContractEvent(name = "paillierMul")
    String paillierMul(String pubkey, String cipher, int scalar);

    /**
     * 秘密恢复
     *
     * @param partsArray
     * @return
     */
    @ContractEvent(name = "secretRecover")
    byte[] secretRecover(String partsArray);
}
