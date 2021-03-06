package org.recap.repository.jpa;

import org.recap.model.jpa.CustomerCodeEntity;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by rajeshbabuk on 18/10/16.
 */
public interface CustomerCodeDetailsRepository extends BaseRepository<CustomerCodeEntity> {

    /**
     * Find the customer code entity by using the customer code.
     *
     * @param customerCode the customer code
     * @return the customer code entity
     */
    CustomerCodeEntity findByCustomerCode(@Param("customerCode")String customerCode);

    /**
     * Find a list of customer code entities by using a list of customer code.
     *
     * @param customerCodes the customer codes
     * @return the list
     */
    List<CustomerCodeEntity> findByCustomerCodeIn(List<String> customerCodes);
}
