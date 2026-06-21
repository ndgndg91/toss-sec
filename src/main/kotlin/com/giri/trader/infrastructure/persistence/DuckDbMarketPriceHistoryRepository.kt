package com.giri.trader.infrastructure.persistence

import com.giri.trader.domain.MarketPriceHistory
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant

@Repository
class DuckDbMarketPriceHistoryRepository(
    @param:Value("\${trader.duckdb.url}") private val duckdbUrl: String
) : MarketPriceHistoryRepository {
    private val log = LoggerFactory.getLogger(DuckDbMarketPriceHistoryRepository::class.java)

    @PostConstruct
    fun initTable() {
        log.info("Initializing DuckDB schema at URL: {}", duckdbUrl)
        try {
            val filePart = duckdbUrl.replace("jdbc:duckdb:", "")
            if (filePart.contains("/")) {
                val dirPath = filePart.substringBeforeLast("/")
                val dirFile = java.io.File(dirPath)
                if (!dirFile.exists()) {
                    dirFile.mkdirs()
                    log.info("Created DuckDB database directory: {}", dirPath)
                }
            }
        } catch (e: Exception) {
            log.warn("Failed to ensure directory structure for DuckDB: {}", e.message)
        }

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS market_price_history (
                        symbol VARCHAR NOT NULL,
                        price DECIMAL(15, 4) NOT NULL,
                        created_at TIMESTAMP NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        log.info("DuckDB market_price_history table verified.")
    }

    private fun getConnection(): Connection {
        return DriverManager.getConnection(duckdbUrl)
    }

    override fun save(history: MarketPriceHistory): MarketPriceHistory {
        val sql = "INSERT INTO market_price_history (symbol, price, created_at) VALUES (?, ?, ?)"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, history.symbol)
                pstmt.setBigDecimal(2, history.price)
                pstmt.setTimestamp(3, Timestamp.from(history.createdAt))
                pstmt.executeUpdate()
            }
        }
        return history
    }

    override fun deleteByCreatedAtBefore(time: Instant): Int {
        val sql = "DELETE FROM market_price_history WHERE created_at < ?"
        return getConnection().use { conn ->
            conn.prepareStatement(sql).use { pstmt ->
                pstmt.setTimestamp(1, Timestamp.from(time))
                pstmt.executeUpdate()
            }
        }
    }

    override fun findAll(): List<MarketPriceHistory> {
        val sql = "SELECT symbol, price, created_at FROM market_price_history ORDER BY created_at DESC"
        val result = mutableListOf<MarketPriceHistory>()
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery(sql).use { rs ->
                    while (rs.next()) {
                        result.add(
                            MarketPriceHistory(
                                symbol = rs.getString("symbol"),
                                price = rs.getBigDecimal("price"),
                                createdAt = rs.getTimestamp("created_at").toInstant()
                            )
                        )
                    }
                }
            }
        }
        return result
    }
}
