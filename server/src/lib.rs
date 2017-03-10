#[macro_use]
extern crate derive_new;
#[macro_use]
extern crate diesel;
#[macro_use]
extern crate diesel_codegen;
extern crate r2d2;
extern crate r2d2_diesel;
extern crate reqwest;
#[macro_use]
extern crate serde_derive;

pub mod models;
pub mod schema;
pub mod telegram_api;

use diesel::pg::PgConnection;
use r2d2::{Config, Pool, PooledConnection};
use r2d2_diesel::ConnectionManager;
use reqwest::Client;
use std::sync::Arc;
use telegram_api::TelegramApi;

#[derive(Clone)]
pub struct Components {
    pub api: Arc<TelegramApi>,
    pub connection_pool: Pool<ConnectionManager<PgConnection>>
}

impl Components {
    pub fn new<S1: Into<String>, S2: Into<String>>(bot_token: S1, database_url: S2) -> Self {
        let client: Client = reqwest::Client::new().unwrap();
        let api = Arc::new(TelegramApi::new(client, bot_token));

        let config = Config::default();
        let connection_manager = ConnectionManager::new(database_url);
        let connection_pool = Pool::new(config, connection_manager)
            .expect("Failed to create connection pool");

        Components { api: api, connection_pool: connection_pool}
    }

    pub fn get_connection(&self) -> PooledConnection<ConnectionManager<PgConnection>> {
        self.connection_pool.get()
            .expect("Unable get connection from connection pool")
    }
}
