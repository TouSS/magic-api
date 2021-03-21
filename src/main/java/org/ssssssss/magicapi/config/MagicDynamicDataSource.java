package org.ssssssss.magicapi.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.ssssssss.magicapi.adapter.DialectAdapter;
import org.ssssssss.magicapi.dialect.Dialect;
import org.ssssssss.magicapi.exception.MagicAPIException;
import org.ssssssss.magicapi.utils.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;

public class MagicDynamicDataSource {

	private static final Logger logger = LoggerFactory.getLogger(MagicDynamicDataSource.class);

	private final Map<String, MagicDynamicDataSource.DataSourceNode> dataSourceMap = new HashMap<>();

	/**
	 * 注册默认数据源
	 */
	public void put(DataSource dataSource) {
		put(null, dataSource);
	}

	/**
	 * 注册数据源（可以运行时注册）
	 *
	 * @param dataSourceKey 数据源Key
	 */
	public void put(String dataSourceKey, DataSource dataSource) {
		put(null, dataSourceKey, dataSourceKey, dataSource);
	}

	/**
	 * 注册数据源（可以运行时注册）
	 *
	 * @param id             数据源ID
	 * @param dataSourceKey  数据源Key
	 * @param datasourceName 数据源名称
	 */
	public void put(String id, String dataSourceKey, String datasourceName, DataSource dataSource) {
		if (dataSourceKey == null) {
			dataSourceKey = "";
		}
		logger.info("注册数据源：{}", StringUtils.isNotBlank(dataSourceKey) ? dataSourceKey : "default");
		this.dataSourceMap.put(dataSourceKey, new MagicDynamicDataSource.DataSourceNode(dataSource, dataSourceKey, datasourceName, id));
		if (id != null) {
			String finalDataSourceKey = dataSourceKey;
			this.dataSourceMap.entrySet().stream()
					.filter(it -> id.equals(it.getValue().getId()) && !finalDataSourceKey.equals(it.getValue().getKey()))
					.findFirst()
					.ifPresent(it -> {
						logger.info("移除旧数据源:{}", it.getValue().getKey());
						this.dataSourceMap.remove(it.getValue().getKey());
					});
		}
	}

	/**
	 * 获取全部数据源
	 */
	public List<String> datasources() {
		return new ArrayList<>(this.dataSourceMap.keySet());
	}

	/**
	 * 获取全部数据源
	 */
	public Collection<DataSourceNode> datasourceNodes() {
		return this.dataSourceMap.values();
	}

	/**
	 * 删除数据源
	 *
	 * @param datasourceKey 数据源Key
	 */
	public boolean delete(String datasourceKey) {
		boolean result = false;
		// 检查参数是否合法
		if (datasourceKey != null && !datasourceKey.isEmpty()) {
			result = this.dataSourceMap.remove(datasourceKey) != null;
		}
		logger.info("删除数据源：{}:{}", datasourceKey, result ? "成功" : "失败");
		return result;
	}

	/**
	 * 获取默认数据源
	 */
	public MagicDynamicDataSource.DataSourceNode getDataSource() {
		return getDataSource(null);
	}

	/**
	 * 获取数据源
	 *
	 * @param datasourceKey 数据源Key
	 */
	public MagicDynamicDataSource.DataSourceNode getDataSource(String datasourceKey) {
		if (datasourceKey == null) {
			datasourceKey = "";
		}
		MagicDynamicDataSource.DataSourceNode dataSourceNode = dataSourceMap.get(datasourceKey);
		Assert.isNotNull(dataSourceNode, String.format("找不到数据源%s", datasourceKey));
		return dataSourceNode;
	}

	public static class DataSourceNode {

		private final String id;

		private final String key;

		private final String name;

		/**
		 * 事务管理器
		 */
		private final DataSourceTransactionManager dataSourceTransactionManager;

		private final JdbcTemplate jdbcTemplate;

		private final DataSource dataSource;

		private Dialect dialect;


		DataSourceNode(DataSource dataSource, String key) {
			this(dataSource, key, key, null);
		}

		DataSourceNode(DataSource dataSource, String key, String name, String id) {
			this.dataSource = dataSource;
			this.key = key;
			this.name = name;
			this.id = id;
			this.dataSourceTransactionManager = new DataSourceTransactionManager(this.dataSource);
			this.jdbcTemplate = new JdbcTemplate(dataSource);
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getKey() {
			return key;
		}

		public JdbcTemplate getJdbcTemplate() {
			return this.jdbcTemplate;
		}

		public DataSourceTransactionManager getDataSourceTransactionManager() {
			return dataSourceTransactionManager;
		}

		public Dialect getDialect(DialectAdapter dialectAdapter) {
			if (this.dialect == null) {
				Connection connection = null;
				try {
					connection = this.dataSource.getConnection();
					this.dialect = dialectAdapter.getDialectFromUrl(connection.getMetaData().getURL());
					if (this.dialect == null) {
						throw new MagicAPIException("自动获取数据库方言失败");
					}
				} catch (Exception e) {
					throw new MagicAPIException("自动获取数据库方言失败", e);
				} finally {
					DataSourceUtils.releaseConnection(connection, this.dataSource);
				}
			}
			return dialect;
		}
	}

	public void setDefault(DataSource dataSource) {
		put(dataSource);
	}

	public void add(String dataSourceKey, DataSource dataSource) {
		put(dataSourceKey, dataSource);
	}
}