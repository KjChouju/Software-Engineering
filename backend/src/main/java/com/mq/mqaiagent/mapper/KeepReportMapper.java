package com.mq.mqaiagent.mapper;

import com.mq.mqaiagent.model.dto.keepReport.KeepReport;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description 针对表【keep_report(健身报告记录表)】的数据库操作Mapper
 * @Entity com.mq.mqaiagent.model.dto.keepReport.KeepReport
 */
@Mapper
public interface KeepReportMapper extends BaseMapper<KeepReport> {

}
