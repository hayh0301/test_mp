package com.example.nihss_mp;
import java.util.LinkedList;

public class MovingAverage {
    private LinkedList<Float> dataList;
    private int maxSize;

    public MovingAverage(int size) {
        this.maxSize = size;
        dataList = new LinkedList<>();
    }

    // 새로운 데이터를 추가하는 메소드
    public void addData(float data) {
        if (dataList.size() >= maxSize) {
            dataList.removeFirst();
        }
        dataList.addLast(data);
    }

    // 현재까지 저장된 데이터들의 평균값을 계산하는 메소드
    public float getAverage() {
        float sum = 0;
        for (float value : dataList) {
            sum += value;
        }
        return sum / dataList.size();
    }
}