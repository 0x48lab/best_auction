# Cloud Integration Documentation

## Overview

The Best Auction plugin includes cloud integration features that allow you to connect your Minecraft server to a cloud service for event tracking, data synchronization, and cross-server functionality. This integration uses a Cloudflare Workers-based API to handle events and synchronize auction data.

## Features

- **Real-time Event Tracking**: Send auction events (listings, bids, sales) to the cloud in real-time
- **Data Synchronization**: Sync existing auction data to the cloud for backup or analytics
- **Authentication**: Secure API token-based authentication
- **Automatic Retry**: Failed requests are automatically retried with configurable delays
- **Event Filtering**: Choose which event types to send to the cloud
- **Batch Processing**: Efficient batch synchronization for large datasets

## Configuration

The cloud integration settings are configured in `config.yml` under the `cloud` section:

```yaml
# Cloud Integration Settings
cloud:
  # Enable cloud event reporting
  enabled: false
  
  # Cloudflare Workers base URL
  base_url: "https://your-worker.your-subdomain.workers.dev"
  
  # API token for authentication (REQUIRED - obtain from cloud dashboard)
  api_token: ""
  
  # Server identifier (used to distinguish multiple servers)
  server_id: "default-server"
  
  # Authentication settings
  auth:
    # Auto-validate token on startup
    validate_on_startup: true
    
    # Re-validate token interval (in minutes, 0 = disabled)
    revalidate_interval: 60
  
  # Data synchronization settings
  sync:
    # Auto-sync existing data on first startup (when token is valid)
    auto_sync_on_startup: true
    
    # Batch size for initial data sync (number of auctions per request)
    batch_size: 50
    
    # Delay between batch requests in milliseconds
    batch_delay: 1000
    
    # Sync existing bids along with auctions
    include_bids: true
    
    # Force full resync (ignores last sync timestamp)
    force_full_sync: false
  
  # HTTP request timeout in milliseconds
  timeout: 5000
  
  # Number of retry attempts for failed requests
  retry_attempts: 3
  
  # Delay between retry attempts in milliseconds
  retry_delay: 1000
  
  # Events to send to cloud (can disable specific events)
  events:
    item_listed: true
    bid_placed: true
    bid_cancelled: true
    auction_cancelled: true
    item_sold: true
```

## API Endpoints

The cloud integration communicates with the following API endpoints:

### Authentication
- **POST** `{base_url}/api/auth/validate` - Validate API token
  - Headers: `Authorization: Bearer {api_token}`
  - Returns: 200 OK if valid, 401 if invalid

### Events
- **POST** `{base_url}/api/events` - Send auction events
  - Headers: `Authorization: Bearer {api_token}`, `Content-Type: application/json`
  - Body: Event data in JSON format

### Synchronization
- **POST** `{base_url}/api/sync` - Batch synchronize auction data
  - Headers: `Authorization: Bearer {api_token}`, `Content-Type: application/json`  
  - Body: Batch sync request with auction data

## Event Types

The following event types are tracked and sent to the cloud:

### ITEM_LISTED
Sent when a player lists an item for auction.
```json
{
  "event_type": "ITEM_LISTED",
  "server_id": "server-1",
  "timestamp": "2024-12-18T10:30:00Z",
  "auction_id": 123,
  "data": {
    "seller_uuid": "uuid-string",
    "seller_name": "PlayerName",
    "item_name": "Diamond Sword",
    "item_type": "DIAMOND_SWORD",
    "quantity": 1,
    "start_price": 1000,
    "buyout_price": 5000
  }
}
```

### BID_PLACED
Sent when a player places a bid on an auction.
```json
{
  "event_type": "BID_PLACED",
  "server_id": "server-1",
  "timestamp": "2024-12-18T10:35:00Z",
  "auction_id": 123,
  "data": {
    "bidder_uuid": "uuid-string",
    "bidder_name": "BidderName",
    "bid_amount": 1500,
    "previous_price": 1000
  }
}
```

### BID_CANCELLED
Sent when a player cancels their bid.
```json
{
  "event_type": "BID_CANCELLED",
  "server_id": "server-1",
  "timestamp": "2024-12-18T10:40:00Z",
  "auction_id": 123,
  "data": {
    "bidder_uuid": "uuid-string",
    "bidder_name": "BidderName",
    "refund_amount": 1500
  }
}
```

### AUCTION_CANCELLED
Sent when an auction is cancelled by the seller.
```json
{
  "event_type": "AUCTION_CANCELLED",
  "server_id": "server-1",
  "timestamp": "2024-12-18T10:45:00Z",
  "auction_id": 123,
  "data": {
    "seller_uuid": "uuid-string",
    "seller_name": "SellerName",
    "reason": "cancelled_by_seller"
  }
}
```

### ITEM_SOLD
Sent when an auction completes with a sale.
```json
{
  "event_type": "ITEM_SOLD",
  "server_id": "server-1",
  "timestamp": "2024-12-18T11:00:00Z",
  "auction_id": 123,
  "data": {
    "seller_uuid": "seller-uuid",
    "seller_name": "SellerName",
    "buyer_uuid": "buyer-uuid",
    "buyer_name": "BuyerName",
    "final_price": 5000,
    "was_buyout": true
  }
}
```

## Cloud Commands

The plugin provides several commands for managing cloud integration:

### Basic Commands
- `/auction cloud status` - Show cloud integration status
- `/auction cloud validate` - Validate the API token
- `/auction cloud sync [force]` - Synchronize auction data
  - Without `force`: Performs incremental sync
  - With `force`: Performs full resync

### Admin Commands (Console Only)
- `/auction cloud gettoken` - Get the configuration instructions for obtaining an API token
- `/auction cloud settoken <token>` - Set and validate a new API token

### Status Information
The status command shows:
- Cloud enabled/disabled state
- Token validation status
- Queue size (pending events)
- Last synchronization status

## Setup Instructions

1. **Enable Cloud Integration**
   ```yaml
   cloud:
     enabled: true
   ```

2. **Set Base URL**
   ```yaml
   cloud:
     base_url: "https://your-worker.your-subdomain.workers.dev"
   ```

3. **Obtain API Token**
   - Run `/auction cloud gettoken` in console for instructions
   - Visit your cloud dashboard to generate a token
   - Set the token using `/auction cloud settoken <token>`

4. **Configure Server ID**
   ```yaml
   cloud:
     server_id: "survival-server-1"
   ```

5. **Test Connection**
   - Run `/auction cloud validate` to test the connection
   - Check `/auction cloud status` to verify everything is working

## Data Synchronization

### Automatic Synchronization
When `auto_sync_on_startup` is enabled, the plugin will:
1. Validate the API token on startup
2. Check if initial sync has been performed
3. If not, sync all active auctions to the cloud
4. Store sync status in the database

### Manual Synchronization
Use `/auction cloud sync` to manually trigger synchronization:
- Syncs all active auctions
- Includes bid history if `include_bids` is enabled
- Processes in batches to avoid overwhelming the API

### Sync Data Format
Each auction is synchronized with the following data:
- Auction ID, seller info, item details
- Pricing information (start, buyout, current)
- Category and listing fees
- Creation and expiration timestamps
- Bid history (if enabled)
- Item metadata (lore, enchantments, durability)

## Security Considerations

1. **API Token**: Keep your API token secure and never share it
2. **HTTPS**: All communication uses HTTPS encryption
3. **Authentication**: Every request includes Bearer token authentication
4. **Server ID**: Use unique server IDs to prevent data conflicts

## Troubleshooting

### Token Validation Failed
- Verify the token is correct
- Check the base URL is accessible
- Ensure the cloud service is running

### Events Not Sending
- Check cloud integration is enabled
- Verify token is valid (`/auction cloud validate`)
- Check specific event types are enabled in config
- Review server logs for error messages

### Sync Issues
- Check batch size isn't too large
- Increase timeout for slow connections
- Verify server has internet access
- Check for firewall restrictions

## Performance Impact

The cloud integration is designed to minimize performance impact:
- Events are queued and sent asynchronously
- Failed requests don't block gameplay
- Batch processing reduces API calls
- Configurable timeouts prevent hanging

## Database Tables

The plugin stores sync status in the `cloud_sync_status` table:
- `server_id`: Server identifier
- `sync_type`: "full" or "incremental"
- `last_sync_at`: Timestamp of last sync
- `synced_auctions`: Number of auctions synced
- `synced_bids`: Number of bids synced
- `status`: "in_progress", "completed", or "failed"
- `error_message`: Error details if failed