package com.deeprobotics.station.openapi.sdk;

import com.deeprobotics.station.openapi.sdk.api.CameraApi;
import com.deeprobotics.station.openapi.sdk.api.DogApi;
import com.deeprobotics.station.openapi.sdk.api.InventoryApi;
import com.deeprobotics.station.openapi.sdk.api.ResultApi;
import com.deeprobotics.station.openapi.sdk.api.SystemApi;
import com.deeprobotics.station.openapi.sdk.api.TaskApi;
import com.deeprobotics.station.openapi.sdk.api.TaskTemplateApi;

/**
 * 智巡平台 OpenAPI SDK 的客户侧统一入口。
 *
 * <p>这个接口刻意只暴露业务域分组，而不暴露具体 HTTP 客户端、签名实现或平台内部类型。
 * 这样客户项目在升级底层 transport、切换鉴权方式或使用 shaded 包解决依赖冲突时，公共调用形态保持稳定。</p>
 *
 * <p>调用方通过 {@link StationOpenApiClients#builder()} 创建实例。默认 transport 由 classpath 中的 SPI
 * provider 自动装配；高级客户也可以显式注入 transport。应用关闭时应调用 {@link #close()} 释放 SDK
 * 创建并持有的底层资源。</p>
 */
public interface StationOpenApiClient extends AutoCloseable {

    /**
     * 台账与基础资源查询分组，例如狗、地图、点位、机巢和摄像头流。
     *
     * @return 与当前客户端共享配置和 transport 的台账 API
     */
    InventoryApi inventory();

    /**
     * 四足狗状态、告警、坐标查询和设备控制分组。
     *
     * @return 与当前客户端共享配置和 transport 的四足狗 API
     */
    DogApi dog();

    /**
     * 摄像机与云台控制分组。控制类操作默认不重试，避免重复下发物理设备命令。
     *
     * @return 与当前客户端共享配置和 transport 的摄像机 API
     */
    CameraApi camera();

    /**
     * 任务模板查询、创建和删除分组。模板写操作属于业务写入，默认不重试。
     *
     * @return 与当前客户端共享配置和 transport 的任务模板 API
     */
    TaskTemplateApi taskTemplate();

    /**
     * 任务下发、暂停、恢复、取消和任务分页查询分组。
     *
     * @return 与当前客户端共享配置和 transport 的任务执行 API
     */
    TaskApi task();

    /**
     * 巡检结果查询与文件下载分组。文件下载返回二进制流，不按普通 {@code R<T>} 成功响应处理。
     *
     * @return 与当前客户端共享配置和 transport 的巡检结果 API
     */
    ResultApi result();

    /**
     * 系统辅助接口分组，例如服务器时间。{@code /remoteApi/time} 没有业务字段，但仍需要鉴权。
     *
     * @return 与当前客户端共享配置和 transport 的系统 API
     */
    SystemApi system();

    /**
     * 释放 SDK 持有的连接池、线程或其他 transport 资源。
     *
     * <p>如果 transport 由 SDK 创建，close 应负责释放；
     * 如果由客户注入，默认不关闭客户共享对象，避免影响客户应用中其他 HTTP 调用。</p>
     *
     * <p>重复调用 close 是幂等的，但 close 不与正在执行的业务调用做排空协调。应用关闭时应先停止接收
     * 新任务并等待在途 SDK 调用结束，再关闭客户端；close 返回后不得继续调用业务 API。</p>
     */
    @Override
    void close();
}
