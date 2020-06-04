package ai_algo

import (
	"fmt"
	"strconv"
)

func addNum(base string, n int) {
	if n <= 1 {
		for i := 0; i < 10; i++ {
			fmt.Println(base + strconv.Itoa(i))
		}
		return
	}

	for i := 0; i < 10; i++ {
		var newBase string
		if base == "" && i == 0 {
			newBase = ""
		} else {
			newBase = base + strconv.Itoa(i)
		}
		addNum(newBase, n-1)
	}
}
