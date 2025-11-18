use core_lib::DiscoveryTimingConfig;

#[derive(Clone, Debug)]
pub struct NodeConfig {
    pub bind_host: String,
    pub bind_port: u16,
    pub public_host: Option<String>,
    pub public_port: Option<u16>,
    pub timing: DiscoveryTimingConfig,
    pub global_registry_urls: Vec<String>,
}

impl NodeConfig {
    /// Canonical API base, e.g. http://host:port/api/v1
    pub fn canonical_address(&self) -> String {
        let host = self
            .public_host
            .clone()
            .unwrap_or_else(|| self.bind_host.clone());
        let port = self.public_port.unwrap_or(self.bind_port);
        format!("http://{host}:{port}/api/v1")
    }
}
