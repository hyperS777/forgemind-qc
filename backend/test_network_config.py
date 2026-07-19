import importlib
import os


def test_default_bind_host_is_all_interfaces(monkeypatch):
    monkeypatch.delenv("FORGEMIND_HOST", raising=False)
    module = importlib.import_module("main")
    assert module.get_bind_host() == "0.0.0.0"


def test_bind_host_can_be_overridden(monkeypatch):
    monkeypatch.setenv("FORGEMIND_HOST", "192.168.1.10")
    module = importlib.import_module("main")
    assert module.get_bind_host() == "192.168.1.10"
