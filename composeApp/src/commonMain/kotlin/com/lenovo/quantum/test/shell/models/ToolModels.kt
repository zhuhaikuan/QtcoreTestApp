package com.lenovo.quantum.test.shell.models

data class ToolServer(
    val name: String,
    val tools: List<ToolDescriptor>
)

data class ToolDescriptor(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val argsCount: Int
)

data class ToolCategory(
    val name: String,
    val tools: List<ToolDescriptor>
)