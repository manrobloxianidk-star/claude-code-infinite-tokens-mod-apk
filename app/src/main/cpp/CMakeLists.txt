cmake_minimum_required(VERSION 3.22.1)
project(infinite_tokens LANGUAGES CXX)

add_library(infinite_tokens SHARED bootstrap.cpp)
target_compile_features(infinite_tokens PRIVATE cxx_std_17)
