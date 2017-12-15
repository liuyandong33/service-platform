package build.dream.platform.mappers;

import build.dream.common.saas.domains.Order;
import build.dream.common.saas.domains.OrderDetail;
import build.dream.common.utils.SearchModel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    long insert(OrderDetail orderDetail);
    long insertAll(List<OrderDetail> orderDetails);
    OrderDetail find(SearchModel searchModel);
    List<OrderDetail> findAll(SearchModel searchModel);
    List<OrderDetail> findAllPaged(SearchModel searchModel);
}
