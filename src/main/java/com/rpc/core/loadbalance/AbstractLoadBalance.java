package com.rpc.core.loadbalance;

import java.util.List;

/**
 *
 * @author Jeremy
 */
public abstract class AbstractLoadBalance implements ILoadBalance{

    /**
     * 客户端id选择
     * @param clientIds
     * @return
     */
    @Override
    public int select(List<Integer> clientIds){
        final int size = clientIds.size();
        if (size == 0){
            return 0;
        }else if (size == 1){
            return clientIds.get(0);
        }
        return doSelect(clientIds);
    }

    /**
     * 处理选择
     * @param clientIds
     * @return
     */
    public abstract int doSelect(List<Integer> clientIds);

}
