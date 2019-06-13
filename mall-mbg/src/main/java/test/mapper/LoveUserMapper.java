package test.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import test.model.LoveUser;
import test.model.LoveUserExample;

public interface LoveUserMapper {
    int countByExample(LoveUserExample example);

    int deleteByExample(LoveUserExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(LoveUser record);

    int insertSelective(LoveUser record);

    List<LoveUser> selectByExampleWithBLOBs(LoveUserExample example);

    List<LoveUser> selectByExample(LoveUserExample example);

    LoveUser selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") LoveUser record, @Param("example") LoveUserExample example);

    int updateByExampleWithBLOBs(@Param("record") LoveUser record, @Param("example") LoveUserExample example);

    int updateByExample(@Param("record") LoveUser record, @Param("example") LoveUserExample example);

    int updateByPrimaryKeySelective(LoveUser record);

    int updateByPrimaryKeyWithBLOBs(LoveUser record);

    int updateByPrimaryKey(LoveUser record);
}