# MLKit Docker Environment

This repository contains a Docker setup for jumpstarting creating models for MLKit via Jupyter Notebooks, with specific versions of essential machine learning tools, including `skl2onnx`, `onnx`, `onnxruntime`, and others. The setup also includes example notebooks from the [Mendix ML Kit example app repository](https://github.com/mendix/mlkit-example-app/tree/main/notebooks).

## Prerequisites

- Docker: Make sure Docker is installed on your machine. You can download Docker from [here](https://www.docker.com/products/docker-desktop).

## Setup

Follow these steps to build and run the Docker container:

1. **Clone the Repository**:
    ```sh
    git clone https://github.com/mendix/mlkit-example-app.git
    cd mlkit-example-app/environments
    ```

2. **Build the Docker Image**:

    Depending on your Mendix version, you may want to choose between `requirements-latest.txt` or `requirements-lts.txt`
    For the example below, let's assume you have the latest LTS version
    ```sh
    docker build REQUIREMENTS_FILE=requirements-lts.txt  -t mlkit-onnx-env .
    ```

3. **Run the Docker Container**:
    ```sh
    docker run -p 8888:8888 mlkit-onnx-env
    ```

4. **Access Jupyter Notebook**:
    Open a web browser and go to `http://localhost:8888`. You will see the Jupyter Notebook interface, and you can find the example notebooks under the `notebooks` directory.

## Dockerfile Explanation

The `Dockerfile` sets up the environment with the following steps:

1. **Base Image**: Uses `python:3.6-slim` as the base image.
2. **Working Directory**: Sets `/workspace` as the working directory.
3. **Install Jupyter Notebook**: Installs Jupyter Notebook version `6.1.5`.
4. **Install Required Packages**: Installs specific versions of machine learning packages and their dependencies.
5. **Clone Example Notebooks**: Clones the Mendix ML Kit example app repository and copies the `notebooks` directory.
6. **Expose Port**: Exposes port `8888` for accessing Jupyter Notebook.
7. **Start Jupyter Notebook**: Sets the command to start Jupyter Notebook on container startup.

## Example Notebooks

The following example notebooks from the Mendix ML Kit example app are included:

- `iris_lr.ipynb`
- `mnist_cnn.ipynb`

These notebooks demonstrate various machine learning tasks and can be used to test and validate the installed packages.

## Installed Packages

Check the requirements-env.txt files in this folder.

## Troubleshooting

If you encounter any issues while building or running the Docker container, try the following steps:

- Ensure Docker is installed and running on your machine.
- Verify that your system meets the prerequisites.
- Check for any typos or errors in the Docker commands.

For further assistance, you can refer to the [Docker documentation](https://docs.docker.com/).

## Contributing

If you would like to contribute to this project, please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature-branch`).
3. Make your changes and commit them (`git commit -am 'Add new feature'`).
4. Push to the branch (`git push origin feature-branch`).
5. Create a new Pull Request.

## License

This project is licensed under the MIT License. See the `LICENSE` file for more details.

---

By following these instructions, you will have a Docker environment set up to start your journey with MLKit via Jupyter Notebooks with the specified versions of machine learning tools and the example notebooks included. Happy coding!