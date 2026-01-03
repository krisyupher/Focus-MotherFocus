from typing import Optional, Dict, Any, List
from memory_mcp import MemoryMCP

class MemoryKB:
	def __init__(self, api_url: str = "http://localhost:5000", api_key: Optional[str] = None):
		self.mem = MemoryMCP(api_url=api_url, api_key=api_key)

	def is_available(self) -> bool:
		return self.mem.is_available()

	def store_agreement(self, agreement_id: str, participants: List[str], content: str, confidence: float = 1.0) -> Any:
		props = {"id": agreement_id, "type":"agreement", "participants": participants, "content": content, "confidence": confidence}
		return self.mem.create_node(props)

	def store_pattern(self, pattern_id: str, description: str, evidence: Optional[List[Dict]] = None, confidence: float = 0.75) -> Any:
		props = {"id": pattern_id, "type":"pattern", "description": description, "evidence": evidence or [], "confidence": confidence}
		return self.mem.create_node(props)

	def link_entities(self, source_id: str, target_id: str, relation: str = "related_to", properties: Optional[Dict] = None) -> Any:
		props = {"source": source_id, "target": target_id, "relation": relation}
		if properties: props["properties"] = properties
		try:
			return self.mem._request("POST", "/edges", payload=props)
		except Exception:
			node = self.mem.get_node(source_id)
			node_props = node if isinstance(node, dict) else {}
			links = node_props.get("links", [])
			links.append({"target": target_id, "relation": relation, **(properties or {})})
			return self.mem.create_node({"id": source_id, **node_props, "links": links})

	def query_patterns(self, q: Optional[str] = None, limit: int = 50) -> Any:
		return self.mem.query_patterns(q=q, limit=limit)

	def export_snapshot(self, save_path: Optional[str] = None) -> bytes:
		return self.mem.export_graph(save_path=save_path)
