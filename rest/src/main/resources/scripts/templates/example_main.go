package main

import (
	"context"
	"fmt"

	"github.com/zstackio/zsphere-sdk-go-v2/pkg/client"
	"github.com/zstackio/zsphere-sdk-go-v2/pkg/param"
)

func main() {
	// 创建客户端配置
	// 方式1: 使用完整配置
	// config := client.NewZSConfig("YOUR_ZSTACK_API_ENDPOINT", 8080, "/zstack").
	// 		LoginAccount("admin", "password").
	// 		Debug(true)

	// 方式2: 使用默认配置（推荐）
	config := client.DefaultZSConfig("YOUR_ZSTACK_API_ENDPOINT").
		LoginAccount("admin", "password").
		Debug(true)

	// 初始化客户端
	cli := client.NewZSClient(config)
	ctx := context.Background()
	// 登录
	fmt.Println("正在登录...")
	sessionView, err := cli.Login(ctx)
	if err != nil {
		fmt.Printf("登录失败: %v\n", err)
		return
	}
	fmt.Printf("登录成功！Session UUID: %s\n", sessionView.Uuid)

	// 查询虚拟机列表
	fmt.Println("\n开始查询虚拟机...")
	queryParams := param.NewQueryParam()
	queryParams.Limit(10)
	vms, err := cli.QueryVmInstance(ctx, &queryParams)
	if err != nil {
		fmt.Printf("查询失败: %v\n", err)
		return
	}

	fmt.Printf("查询成功！共找到 %d 台虚拟机\n", len(vms))
	for i, vm := range vms {
		fmt.Printf("[%d] VM: %s, UUID: %s, State: %s\n", i+1, vm.Name, vm.Uuid, vm.State)
	}

	// 获取单个虚拟机详情（如果有虚拟机的话）
	if len(vms) > 0 {
		fmt.Printf("\n获取第一台虚拟机详情...\n")
		vmDetail, err := cli.GetVmInstance(ctx, vms[0].Uuid)
		if err != nil {
			fmt.Printf("获取虚拟机详情失败: %v\n", err)
		} else {
			fmt.Printf("虚拟机详情: Name=%s, UUID=%s, State=%s, CPUs=%d, Memory=%d\n",
				vmDetail.Name, vmDetail.Uuid, vmDetail.State, vmDetail.CpuNum, vmDetail.MemorySize)
		}
	}

	// 查询镜像列表
	fmt.Println("\n查询镜像列表...")
	imageParams := param.NewQueryParam()
	imageParams.Limit(5)
	images, err := cli.QueryImage(ctx, &imageParams)
	if err != nil {
		fmt.Printf("查询镜像失败: %v\n", err)
	} else {
		fmt.Printf("共找到 %d 个镜像\n", len(images))
		for i, img := range images {
			fmt.Printf("[%d] Image: %s, UUID: %s, Format: %s\n", i+1, img.Name, img.Uuid, img.Format)
		}
	}

	// 验证会话是否有效
	fmt.Println("\n验证会话...")
	valid, err := cli.ValidateSession(ctx)
	if err != nil {
		fmt.Printf("验证会话失败: %v\n", err)
	} else {
		fmt.Printf("会话有效性: %v\n", valid)
	}

	// 登出
	fmt.Println("\n正在登出...")
	if err := cli.Logout(ctx); err != nil {
		fmt.Printf("登出失败: %v\n", err)
	} else {
		fmt.Println("登出成功！")
	}
}
