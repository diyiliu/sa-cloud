CREATE TABLE Role(
    Id INT AUTO_INCREMENT PRIMARY KEY,
    NAME VARCHAR(20) NOT NULL COMMENT '角色名称',
    Description VARCHAR(200) COMMENT '角色描述'	,
    UserType TINYINT UNSIGNED NOT NULL COMMENT '用户类型(1管理员;2经销商;3客户)'
);

ALTER TABLE Role COMMENT='角色表';

CREATE TABLE Users(
    Id INT AUTO_INCREMENT PRIMARY KEY,
    LoginName VARCHAR(16) NOT NULL COMMENT '登录名',
    Pwd CHAR(32) NOT NULL COMMENT '密码',
    RoleId INT COMMENT '角色Id',
    UserType TINYINT UNSIGNED NOT NULL COMMENT '用户类型(1管理员;2经销商;3客户)',
    CreateTime TIMESTAMP NOT NULL COMMENT '创建时间',
    CreateUserId INT COMMENT '创建人',
    NAME VARCHAR(50) NOT NULL COMMENT '管理员姓名/经销商名/客户名',
    Tel VARCHAR(50) COMMENT '联系电话',
    Address VARCHAR(100) COMMENT '客户/经销商地址'
);

ALTER TABLE Users COMMENT='用户表';

CREATE UNIQUE INDEX IUX_Users_LoginName ON Users (LoginName);


CREATE TABLE ProductModel(
    Id INT AUTO_INCREMENT PRIMARY KEY,
    NAME VARCHAR(32) NOT NULL COMMENT '产品型号',
    RatedPressure VARCHAR(20) COMMENT '额定排气压力',
    RatedFlow VARCHAR(20) COMMENT '额定流量',
    MotorPower VARCHAR(20) COMMENT '电机功率',
    CompressorNum INT COMMENT '压缩机数',
    WorkMode VARCHAR(20) COMMENT '工作方式',
    Weight VARCHAR(20) COMMENT '重量',
    Dimensions VARCHAR(50) COMMENT '外形尺寸'
);

CREATE UNIQUE INDEX IUX_ProductModel_Name ON ProductModel (NAME);

ALTER TABLE ProductModel COMMENT='产品型号';


CREATE TABLE Equipment(
    Id INT AUTO_INCREMENT PRIMARY KEY,
    AgentId INT COMMENT '经销商',
    CustomerId INT COMMENT '客户',
    DtuId VARCHAR(32) COMMENT 'DTU Id',
    EquipmentCode VARCHAR(32) NOT NULL COMMENT '机组编码',
    ElectricCode VARCHAR(32) COMMENT '电柜编码',
    ModelId INT NOT NULL COMMENT '产品型号Id',
    FactoryDate DATE COMMENT '出厂日期',
    Saller VARCHAR(20) COMMENT '尚爱销售经理',
    SallerTel VARCHAR(50) COMMENT '尚爱销售经理联系方式',
    CustomerTech VARCHAR(20) COMMENT '客户技术人员',
    CustomerTechTel VARCHAR(20) COMMENT '客户技术人员联系方式',
    WarrantyBegin DATE COMMENT '报修起始日期',
    WarrantyEnd DATE COMMENT '报修截止日期',
    CreateTime TIMESTAMP NOT NULL COMMENT '注册时间'
);

CREATE INDEX IX_Equipment_Agent ON Equipment (AgentId);
CREATE INDEX IX_Equipment_Customer ON Equipment (CustomerId);

ALTER TABLE Equipment COMMENT='设备表';

ALTER TABLE Equipment ADD(
    Address VARCHAR(100) COMMENT '设备地址'
);



CREATE TABLE CallRepair(
    Id INT AUTO_INCREMENT PRIMARY KEY,
    UserId INT COMMENT '报修用户',
    EquipmentCode VARCHAR(32) NOT NULL COMMENT '机组编码',
    Linkman VARCHAR(20) NOT NULL COMMENT '联系人',
    Tel VARCHAR(50) NOT NULL COMMENT '联系电话',
    Address VARCHAR(100) NOT NULL COMMENT '设备地址',
    Description VARCHAR(500) NOT NULL COMMENT '故障描述',
    CreateTime TIMESTAMP NOT NULL COMMENT '报修时间',
    ReceptionUserId INT COMMENT '接报人'
);

ALTER TABLE CallRepair COMMENT '服务报修单';

CREATE INDEX IX_CallRepair_User ON CallRepair (UserId);
CREATE INDEX IX_CallRepair_Equipment ON CallRepair (EquipmentCode);


CREATE TABLE ServiceAssignment(
    Id INT AUTO_INCREMENT PRIMARY KEY,
    CallRepairId INT COMMENT '保修单Id',
    StaffUserId INT COMMENT '服务人员',
    AssignTime DATETIME NOT NULL COMMENT '派工时间',
    Fault VARCHAR(500) COMMENT '故障现象',
    Analysis VARCHAR(500) COMMENT '故障分析',
    Solution VARCHAR(500) COMMENT '解决方案',
    STATUS TINYINT UNSIGNED NOT NULL COMMENT '派工单状态(1已派工;2完工;)',
    IsResolved TINYINT UNSIGNED COMMENT '结果反馈(0已解决;1未解决)',
    FeedbackTime DATETIME COMMENT '反馈时间'
);

CREATE INDEX IX_ServiceAssignment_Staff ON ServiceAssignment (StaffUserId, AssignTime);
CREATE INDEX IX_ServiceAssignment_CallRepair ON ServiceAssignment (CallRepairId);

ALTER TABLE ServiceAssignment COMMENT '服务派工单';


CREATE TABLE DailyWorkHour(
    EquipmentId INT NOT NULL COMMENT '设备Id',
    DAY DATE NOT NULL COMMENT '日期',
    Hours DECIMAL(4,2) NOT NULL COMMENT '工作小时',
    PRIMARY KEY (EquipmentId, DAY)
);

ALTER TABLE DailyWorkHour COMMENT='每日工作时间';


CREATE TABLE Equipment_Info(
    EquipmentId INT PRIMARY KEY,
    DtuStatus TINYINT COMMENT '在线状态(1在线;0离线)',
    LastTime DATETIME COMMENT '数据回传时间',
    MainCurrent FLOAT COMMENT '主机电流(A)',
    PowerSupplyVoltage FLOAT COMMENT '供电电压(V)',
    LubricatingOilPressure FLOAT COMMENT '润滑油压(Mpa)',
    CoolingWaterPressure FLOAT COMMENT '冷却水压(Mpa)',
    FirstLevelPressure FLOAT COMMENT '一级排压(Mpa)',
    SecondLevelPressure FLOAT COMMENT '二级排压(Mpa)',
    ThirdLevelPressure FLOAT COMMENT '三级排压(Mpa)',
    FourthLevelPressure FLOAT COMMENT '四级排压(Mpa)',
    FirstLevelTemperature FLOAT COMMENT '一级排温(℃)',
    SecondLevelTemperature FLOAT COMMENT '二级排温(℃)',
    ThirdLevelTemperature FLOAT COMMENT '三级排温(℃)',
    FourthLevelTemperature FLOAT COMMENT '四级排温(℃)',
    FirstWaterTemperature FLOAT COMMENT '一缸水温(℃)',
    SecondWaterTemperature FLOAT COMMENT '二缸水温(℃)',
    ThirdWaterTemperature FLOAT COMMENT '三缸水温(℃)',
    FourthWaterTemperature FLOAT COMMENT '四缸水温(℃)',
    LubricatingOilTemperature FLOAT COMMENT '润滑油温(℃)',
    InletWaterTemperature FLOAT COMMENT '进水温度(℃)',
    InstrumentPressure FLOAT COMMENT '仪表气压(Mpa)',
    FirstBushingTemperature FLOAT COMMENT '轴瓦一温(℃)',
    SecondBushingTemperature FLOAT COMMENT '轴瓦二温(℃)',
    ThirdBushingTemperature FLOAT COMMENT '轴瓦三温(℃)',
    FourthBushingTemperature FLOAT COMMENT '轴瓦四温(℃)',
    FifthBushingTemperature FLOAT COMMENT '轴瓦五温(℃)',
    ShaftExtendTemperature FLOAT COMMENT '轴伸端温(℃)',
    ShaftEndTemperature FLOAT COMMENT '轴末端温(℃)',
    FirstStatorTemperature FLOAT COMMENT '定子一温(℃)',
    SecondStatorTemperature FLOAT COMMENT '定子二温(℃)',
    ThirdStatorTemperature FLOAT COMMENT '定子三温(℃)',
    WorkingFrequency FLOAT COMMENT '运行频率(Hz)',
    PbStartIn TINYINT COMMENT '主机启动输入',
    PbStopIn TINYINT COMMENT '主机停止输入',
    PbOilPumpIn TINYINT COMMENT '油泵启停输入',
    PbEmergencyIn TINYINT COMMENT '紧急停机输入',
    FbStartIn TINYINT COMMENT '启动反馈输入',
    AlarmInverterIn TINYINT COMMENT '变频器报警输入',
    FaultInverterIn TINYINT COMMENT '变频器故障输入',
    OverLoadOilPumpIn TINYINT COMMENT '油泵过载输入',
    LowPressureIn TINYINT COMMENT '油压低输入',
    LowWaterIn TINYINT COMMENT '水流低输入',
    OilPressureDifferenceIn TINYINT COMMENT '油压压差输入',
    LoadValveControlHalfIn TINYINT COMMENT '加载阀控制50%输入',
    LoadValveControlFullIn TINYINT COMMENT '加载阀控制100%输入',
    FirstValveControlLowStopIn TINYINT COMMENT '低位停止(一级排污阀控制)输入',
    FirstValveControlMiddleStartIn TINYINT COMMENT '中位开启(一级排污阀控制)输入',
    FirstValveControlHighAlarmIn TINYINT COMMENT '高位报警(一级排污阀控制)输入',
    SecondValveControlLowStopIn TINYINT COMMENT '低位停止(二级排污阀控制)输入',
    SecondValveControlMiddleStartIn TINYINT COMMENT '中位开启(二级排污阀控制)输入',
    SecondValveControlHighAlarmIn TINYINT COMMENT '高位报警(二级排污阀控制)输入',
    ThirdValveControlLowStopIn TINYINT COMMENT '低位停止(三级排污阀控制)输入',
    ThirdValveControlMiddleStartIn TINYINT COMMENT '中位开启(三级排污阀控制)输入',
    ThirdValveControlHighAlarmIn TINYINT COMMENT '高位报警(三级排污阀控制)输入',
    FourthValveControlLowStopIn TINYINT COMMENT '低位停止(四级排污阀控制)输入',
    FourthValveControlMiddleStartIn TINYINT COMMENT '中位开启(四级排污阀控制)输入',
    BypassControlOut TINYINT COMMENT '主机旁路控制输出',
    FaultStopOut TINYINT COMMENT '故障停机输出',
    OilPumpRunOut TINYINT COMMENT '油泵运行输出',
    HeatingOut TINYINT COMMENT '电加热输出',
    LoadValve1Out TINYINT COMMENT '加载阀1输出',
    LoadValve2Out TINYINT COMMENT '加载阀2输出',
    FirstPollutionOut TINYINT COMMENT '一级排污输出',
    SecondPollutionOut TINYINT COMMENT '二级排污输出',
    ThirdPollutionOut TINYINT COMMENT '三级排污输出',
    FourthPollutionOut TINYINT COMMENT '四级排污输出',
    FirstWaterInletOut TINYINT COMMENT '一缸进水输出',
    SecondWaterInletOut TINYINT COMMENT '二缸进水输出',
    ThirdWaterInletOut TINYINT COMMENT '三缸进水输出',
    FourthWaterInletOut TINYINT COMMENT '四缸进水输出',
    StopValveOut TINYINT COMMENT '停机放气阀输出',
    FaultOut TINYINT COMMENT '故障指示输出'
);
